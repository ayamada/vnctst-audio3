(ns vnctst.audio3.demo.server
  (:require [ring.middleware.resource :as resources]
            [ring.util.response :as response]
            [clojure.string :as string]
            [hiccup.core :as hiccup]
            [hiccup.page :as page]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            ))

(defn- prevent-cache [path]
  (str path "?" (.getTime (java.util.Date.))))

(def error-404
  {:status 404
   :headers {"Content-Type" "text/html; charset=UTF-8"}
   :body (page/html5 {} [:body [:h1 "404 NOT FOUND"]])})


(defn- error-text [code text]
  {:status code
   :headers {"Content-Type" "text/plain; charset=UTF-8"
             "Pragma" "no-cache"
             "Cache-Control" "no-cache"
             }
   :body text})


(def ^:private title "vnctst-audio3 demo")

;;; TODO: methodも判定に含めるべき
(defn render-app [req]
  {:status 200
   :headers {"Content-Type" "text/html; charset=UTF-8"
             "Pragma" "no-cache"
             "Cache-Control" "no-cache"
             }
   :body (page/html5
           [:head
            [:meta {:http-equiv "X-UA-Compatible", :content "IE=edge"}]
            [:meta {:charset "UTF-8"}]
            [:meta {:name "viewport", :content "width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no"}]
            [:meta {:name "apple-mobile-web-app-capable" :content "yes"}]
            [:meta {:http-equiv "Pragma", :content "no-cache"}]
            [:meta {:http-equiv "Cache-Control", :content "no-cache"}]
            [:title title]
            ;[:link {:href "css/reset.css", :rel "stylesheet", :type "text/css"}]
            ;[:link {:href (prevent-cache "css/default.css"), :rel "stylesheet", :type "text/css"}]
           [:body
            {:onload "vnctst.audio3.demo.bootstrap()"}
            [:h1 title]
            ;; TODO: githubへのリンク等を設置
            ;[:div#the-game [:div#preloading [:div.valign-middle "LOADING"]]]
            [:div
             ;; TODO: ロード中は表示しないようにする
             ;; TODO: ここにボタン類を配置する
             ]
            [:script {:src (prevent-cache "cljs/cl.js")
                      :type "text/javascript"} ""]]
           ])})


(defn- app-handler [req]
  (let [uri (:uri req)]
    (case uri
      "/" (render-app req)
      ;"/hoge" (hoge! req)
      error-404)))









(def content-type-table
  {"html" "text/html; charset=UTF-8"
   "txt" "text/plain; charset=UTF-8"
   "css" "text/css"
   "js" "text/javascript"
   "png" "image/png"
   "jpg" "image/jpeg"
   "ico" "image/x-icon"
   "woff" "application/font-woff"
   "ttf" "application/octet-stream"
   "ttc" "application/octet-stream"
   "ogg" "audio/ogg"
   "mp3" "audio/mpeg"
   "aac" "audio/aac"
   "m4a" "audio/mp4"
   ;; TODO: add more types
   })

;;; IE must needs content-type for css files !!!
(defn- fix-content-type [req res]
  (if (get-in res [:headers "Content-Type"])
    res
    (let [filename (:uri req)
          [_ ext] (re-find #"\.(\w+)$" filename)
          content-type (content-type-table (string/lower-case (or ext "")))]
      ;(println (pr-str :DEBUG filename ext content-type))
      (if content-type
        (response/content-type res content-type)
        res))))

(def handler
  (let [h (resources/wrap-resource app-handler "public")]
    (fn [req]
      (let [res (h req)
            res (response/header res "Cache-Control" "no-cache")
            res (response/header res "Pragma" "no-cache")
            ]
        (fix-content-type req res)))))



