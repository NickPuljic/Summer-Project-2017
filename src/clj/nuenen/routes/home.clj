(ns nuenen.routes.home
  (:require [nuenen.layout :as layout]
            [compojure.core :refer [defroutes GET POST]]
            [ring.util.http-response :as response]
            [ring.util.response :refer [response redirect content-type]]
            [clojure.java.io :as io]
            #_[nuenen.db.core :as db]
            [struct.core :as st]
            [clojure.set :as set]
            [buddy.auth :refer [authenticated? throw-unauthorized]]))

(defn ok [d] {:status 200 :body d})
(defn bad-request [d] {:status 400 :body d})

(defn home-page []
  (layout/render "home.html"))

(defn create-user []
  #_(db/create-user! (assoc {:email "" :password "supersecret"} :timestamp (java.time.LocalDateTime/now)))
  (println "User added"))

(defn validate-user [params]
  #_(if (= nil (db/is-user params)) false true))

(defn authentication-test
  [request]
  (if-not (authenticated? request)
    (throw-unauthorized)
    (response/ok)))

(defn login-authenticate
  [request]
  (let [params (get-in request [:params])
        session (:session request)]
    (if (validate-user params)
      (let [next-url (get-in request [:query-params :next] "/response/Authenticated")
            updated-session (assoc session :identity (-> params :email keyword))]
        (-> (redirect next-url)
            (assoc :session updated-session)))
      (println params))))

(defn logout
  [request]
  (-> (redirect "/response/Loggedout")
      (assoc :session {})))

(defn change-password
  [request]
  (let [params (get-in request [:params])]
    (if-not (= (-> params :newpassword1) (-> params :newpassword2))
      (bad-request "New Passwords Don't Match")
      (if (validate-user params)
        (do
          #_(db/update-password! (set/rename-keys (dissoc params :newpassword2) {:newpassword1 :newpassword}))
          (ok "Success"))
        (bad-request "User Credentials Incorrect")))))

(defroutes home-routes
  (GET "/" []
       (home-page)
       )
  (GET "/docs" []
       (-> (response/ok (-> "docs/docs.md" io/resource slurp))
           (response/header "Content-Type" "text/plain; charset=utf-8")))
  (POST "/login" request (login-authenticate request))
  (GET "/logout" request (logout request))
  (GET "/test" request (println request))
  (GET "/test2" request (authentication-test request))
  (GET "/response/:answer" [answer] (ok answer))
  (POST "/change-password" request (change-password request))
  (GET "/participants" request (response/ok (doall (-> "resources/participants" slurp read-string))))
  (GET "/restatement/:id" [id] (response/ok (doall (-> (str "resources/restatement/" id) slurp read-string))))
  (GET "/snfnew" request (response/ok (doall (-> "resources/snfnew" slurp read-string))))
  )
