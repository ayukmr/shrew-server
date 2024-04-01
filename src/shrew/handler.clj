(ns shrew.handler
  (:require
   [compojure.core :refer [defroutes routes wrap-routes GET POST]]
   [compojure.route :as route]
   [ring.middleware.defaults :refer [api-defaults wrap-defaults]]
   [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
   [ring.middleware.cors :refer [wrap-cors]]
   [ring.util.response :refer [response status]]
   [shrew.database :as db]))

(defn wrap-auth [handler auth-type]
  (fn [{{team :team} :params
        {auth "authorization"} :headers
        :as request}]
    (if (db/auth? team auth-type auth)
      (handler request)
      (status (response "Invalid password") 403))))

(defroutes app-routes
  (-> (routes
        (GET "/points/:team" [team]
          (-> (db/get-points team)
              (response)))

        (GET "/responses/:team" [team]
          (-> (db/get-responses team)
              (response)))

        (POST "/settings/:team" [team :as {{event     :event
                                            points    :points
                                            questions :questions} :body}]
          (db/set-settings! team {:event     event
                                  :points    (into-array points)
                                  :questions (into-array questions)})
          (status nil 200)))
      (wrap-routes wrap-auth :admin))

  (-> (routes
        (GET "/settings/:team" [team]
          (-> (db/get-settings team)
              (response)))

        (POST "/points/:team" [team
                               :as {{match    :match
                                     scouting :scouting
                                     points   :points} :body}]
          (->> (map #(select-keys % [:move :intake :outtake :point]) points)
               (db/add-points! team match scouting))
          (status nil 200))

        (POST "/responses/:team" [team
                                  :as {{match     :match
                                        scouting  :scouting
                                        responses :responses} :body}]
          (->> (map #(select-keys % [:question :response]) responses)
               (db/add-responses! team match scouting))
          (status nil 200)))
      (wrap-routes wrap-auth :scout))

  (GET "/auth/:type/:team" [type team
                            :as {{auth "authorization"} :headers}]
    (-> {:valid (db/auth? team (keyword type) auth)}
        (response)))

  (route/not-found "Not found"))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(def app
  (-> app-routes
      (wrap-json-body {:key-fn keyword})
      (wrap-json-response)
      (wrap-cors :access-control-allow-origin  [#"http://localhost:8080"]
                 :access-control-allow-methods [:get :put :post :delete])
      (wrap-defaults api-defaults)))
