(defproject shrew-server "0.1.0"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [compojure "1.7.1"]
                 [ring/ring-defaults "0.4.0"]
                 [ring/ring-json "0.5.1"]
                 [ring-cors "0.1.13"]
                 [com.github.seancorfield/honeysql "2.6.1126"]
                 [com.github.seancorfield/next.jdbc "1.3.925"]
                 [com.h2database/h2 "2.2.224"]]

  :plugins [[lein-ring "0.12.6"]]

  :ring {:handler shrew-server.handler/app})
