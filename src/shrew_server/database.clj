(ns shrew-server.database
  (:require
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
  (jdbc/execute! ds ["
  CREATE TABLE points (
    id      INTEGER AUTO_INCREMENT PRIMARY KEY,
    scout   VARCHAR(255),
    event   VARCHAR(255),
    match   INTEGER,
    team    INTEGER,
    move    REAL,
    intake  REAL,
    outtake REAL,
    point   VARCHAR(255)
  );

  CREATE TABLE responses (
    id       INTEGER AUTO_INCREMENT PRIMARY KEY,
    scout    VARCHAR(255),
    event    VARCHAR(255),
    match    INTEGER,
    team     INTEGER,
    question VARCHAR(255),
    response VARCHAR(255)
  );

  CREATE TABLE auth (
    team  INTEGER PRIMARY KEY,
    scout VARCHAR(255),
    admin VARCHAR(255)
  );

  CREATE TABLE settings (
    team      INTEGER PRIMARY KEY,
    event     VARCHAR(255),
    points    VARCHAR(255) ARRAY,
    questions VARCHAR(255) ARRAY
  );"]))

(defn auth? [team type pass]
  (->> (-> (h/select [type])
           (h/from   :auth)
           (h/where  [:= :team team])
           (sql/format))
       (jdbc/execute-one! ds)
       (type)
       (= pass)))

(defn create-team! [team scout-pass admin-pass]
  (->> (-> (h/insert-into :auth)
           (h/values [{:team  team
                       :scout scout-pass
                       :admin admin-pass}])
           (sql/format))
       (jdbc/execute! ds))
  (->> (-> (h/insert-into :settings)
           (h/values [{:team      team
                       :event     nil
                       :points    (into-array ["Speaker" "Amp"])
                       :questions (into-array ["How was the team's driver?"])}])
           (sql/format))
       (jdbc/execute! ds)))

(defn get-settings [team]
  (->> (-> (h/select :event :points :questions)
           (h/from   :settings)
           (h/where  [:= :team team])
           (sql/format))
       (jdbc/execute-one! ds)))

(defn get-setting [team key]
  (-> (get-settings team)
      (get key)))

(defn set-setting! [team key value]
  (when (contains? #{:event :points :questions} key)
    (->> (-> (h/update :settings)
             (h/set    {key value})
             (h/where  [:= :team team])
             (sql/format))
         (jdbc/execute! ds))))

(defn set-settings! [team settings]
  (->> (-> (h/update :settings)
           (h/set    settings)
           (h/where  [:= :team team])
           (sql/format))
       (jdbc/execute! ds)))

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
       (jdbc/execute! ds)))

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

(defn add-responses! [scout match team responses]
  (->> (-> (h/insert-into :responses)
           (h/values (add-metadata scout match team responses))
           (sql/format))
       (jdbc/execute! ds)))
