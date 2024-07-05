(ns shrew.database
  (:require
    [clojure.java.io :as io]
    [honey.sql :as sql]
    [honey.sql.helpers :as h]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :refer [as-unqualified-lower-maps ReadableColumn]])
  (:import [java.sql Array]))

(extend-protocol ReadableColumn
  Array
  (read-column-by-label [^Array v _]   (vec (.getArray v)))
  (read-column-by-index [^Array v _ _] (vec (.getArray v))))

(def db {:dbtype "h2" :dbname "data"})
(def ds (-> db
            (jdbc/get-datasource)
            (jdbc/with-options {:builder-fn as-unqualified-lower-maps})))

(defn create-tables! []
  (jdbc/execute! ds [(-> "up.sql"
                         io/resource
                         io/file
                         slurp)]))

(defn check-pass? [team type pass]
  (->> (-> (h/select [type])
           (h/from   :auth)
           (h/where  [:= :team team])
           (sql/format))
       (jdbc/execute-one! ds)
       (type)
       (= pass)))

(defn auth? [team type pass]
  (when (contains? #{:scout :admin} type)
    (if (= type :scout)
      (or (check-pass? team :scout pass)
          (check-pass? team :admin pass))
      (check-pass? team :admin pass))))

(defn create-team! [team scout-pass admin-pass]
  (->> (-> (h/insert-into :auth)
           (h/values [{:team  team
                       :scout scout-pass
                       :admin admin-pass}])
           (sql/format))
       (jdbc/execute! ds))
  (->> (-> (h/insert-into :settings)
           (h/values [{:team   team
                       :event  "Sample Event"
                       :points (into-array ["Speaker" "Amp"])}])
           (sql/format))
       (jdbc/execute! ds))
  (->> (-> (h/insert-into :questions)
           (h/values [{:team team
                       :type "pre"
                       :questions (into-array ["What did they do?"])}])
           (sql/format))
       (jdbc/execute! ds))
  (->> (-> (h/insert-into :questions)
           (h/values [{:team team
                       :type "post"
                       :questions (into-array ["How was the team's driver?"])}])
           (sql/format))
       (jdbc/execute! ds)))

(defn get-settings [team]
  (->> (-> (h/select :event :points)
           (h/from   :settings)
           (h/where  [:= :team team])
           (sql/format))
       (jdbc/execute-one! ds)))

(defn get-setting [team key]
  (-> (get-settings team)
      (get key)))

(defn get-questions [team]
  (->> (-> (h/select :type :questions)
           (h/from :questions)
           (h/where [:= :team team])
           (sql/format))
       (jdbc/execute! ds)
       (map #(identity [(get % :type) (get % :questions)]))
       (into {})))

(defn set-settings! [team settings]
  (->> (-> (h/update :settings)
           (h/set    settings)
           (h/where  [:= :team team])
           (sql/format))
       (jdbc/execute! ds)))

(defn set-questions! [team type questions]
  (when (contains? #{"pre" "post"} type)
    (->> (-> (h/update :questions)
             (h/set {:questions questions})
             (h/where [:= :team team] [:= :type type])
             (sql/format))
         (jdbc/execute! ds))))

(defn get-points [team]
  (->> (-> (h/select [:*])
           (h/from   :points)
           (h/where  [:= :scout team])
           (sql/format))
       (jdbc/execute! ds)))

(defn get-responses [team]
  (->> (-> (h/select [:*])
           (h/from   :responses)
           (h/where  [:= :scout team])
           (sql/format))
       (jdbc/execute! ds)
       (group-by :type)))

(defn add-metadata [scout match team data]
  (map #(merge % {:scout scout
                  :event (get-setting scout :event)
                  :match match
                  :team  team}) data))

(defn add-points! [scout match team points]
  (->> (-> (h/insert-into :points)
           (h/values (add-metadata scout match team points))
           (sql/format))
       (jdbc/execute! ds)))

(defn add-responses! [scout match team type responses]
  (when (contains? #{"pre" "post"} type)
    (->> (-> (h/insert-into :responses)
             (h/values (->> responses
                            (add-metadata scout match team)
                            (map #(merge % {:type type}))))
             (sql/format))
         (jdbc/execute! ds))))
