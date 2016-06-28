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
(def ^:private bg-color "#BFBFBF")
(def ^:private pressed-bg-color "#BFBFBF")

(defn- demo-button [id]
  (let [id (name id)
        desc-id (str (name id) "-desc")]
    [:span [:button {:id id} " "] " - " [:span {:id desc-id} " "]]))

(defn render-app [req]
  (let [github-url "https://github.com/ayamada/vnctst-audio3"
        address [:p [:a {:href github-url
                         :target "_blank"}
                     "(vnctst-audio3 github repos)"]]]
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
              [:style {:type "text/css"}
               "button {font-family:monospace; padding:0.5em; margin:0.2em}"]
              ]
             [:body
              {:onload "vnctst.audio3.demo.bootstrap()"}
              [:div#github-ribbon
               [:a {:href github-url
                    :target "_blank"}
                [:img {:style "position: absolute; top: 0; right: 0; border: 0;"
                       :src "https://camo.githubusercontent.com/365986a132ccd6a44c23a9169022c0b5c890c387/68747470733a2f2f73332e616d617a6f6e6177732e636f6d2f6769746875622f726962626f6e732f666f726b6d655f72696768745f7265645f6161303030302e706e67"
                       :alt "Fork me on GitHub"
                       :data-canonical-src "https://s3.amazonaws.com/github/ribbons/forkme_right_red_aa0000.png"}]]]
              [:h1 title]
              address
              [:div#message "Loading ..."]
              [:div#main {:style "display: none"}
               [:div#version "Version:"]
               [:hr]
               ;; init実行時の情報表示
               [:div
                [:span "以下の引数で初期化を実行しました："]
                [:br]
                [:code#init-info "(init-info)"]]
               [:hr]
               ;; cljs/js切り替えボタン
               [:div
                [:button {:onclick "vnctst.audio3.demo.jsmode(false)"} "cljs向けの表示にする"]
                [:br]
                [:button {:onclick "vnctst.audio3.demo.jsmode(true)"} "js向けの表示にする"]
                ]
               [:hr]
               ;; BGM/ME
               [:div
                "BGM / ME :"
                [:br]
                (demo-button :stop-bgm)
                [:br]
                (demo-button :stop-bgm-0)
                [:br]
                [:br]
                (demo-button :play-bgm-va3)
                [:br]
                (demo-button :play-bgm-drop)
                [:br]
                (demo-button :play-bgm-drop-2)
                [:br]
                [:br]
                (demo-button :play-me-unmei)
                [:br]
                (demo-button :play-me-unmei-2)
                [:br]
                [:br]
                (demo-button :play-bgm-drop-ogg)
                [:br]
                (demo-button :play-bgm-drop-mp3)
                [:br]
                (demo-button :play-me-unmei-ogg)
                ]
               [:hr]
               ;; BGS
               [:div
                "BGS :"
                [:br]
                (demo-button :stop-bgs)
                [:br]
                (demo-button :stop-bgs-0)
                [:br]
                [:br]
                (demo-button :play-bgs-noise)
                ]
               [:hr]
               ;; SE
               [:div
                "SE :"
                [:br]
                (demo-button :play-se-jump)
                [:br]
                (demo-button :play-se-yarare)
                [:br]
                [:br]
                (demo-button :play-se-yarare-ogg)
                ]
               [:hr]
               ;; misc
               [:div
                "Misc :"
                [:br]
                (demo-button :set-volume-master-25)
                [:br]
                (demo-button :set-volume-master-50)
                [:br]
                (demo-button :set-volume-master-100)
                [:br]
                [:br]
                (demo-button :can-play-ogg)
                [:br]
                (demo-button :can-play-mp3)
                [:br]
                (demo-button :can-play-m4a)
                ]
               ;; footer
               [:hr]
               address
               ]
              [:script {:src (prevent-cache "cljs/cl.js")
                        :type "text/javascript"} ""]])}))


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



