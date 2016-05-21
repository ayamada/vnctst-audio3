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

(defn- demo-button [onclick label]
  [:button {:onclick onclick} label])

(defn render-app [req]
  (let [address [:p [:a {:href "https://github.com/ayamada/vnctst-audio3"
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
              ;[:style {:type "text/css"} ""]
              ]
             [:body
              {:onload "vnctst.audio3.demo.bootstrap()"}
              ;; TODO: github ribbon をつける
              [:h1 title]
              address
              [:div#message "Loading ..."]
              ;; TODO: 記述が冗長なので、cljs側でボタンを配置するようにしたい
              ;;       (現状だとラベルがcljs表記でonclickがjs表記なのが嫌。
              ;;       両方をcljs表記で統合したい)
              [:div#main {:style "display: none"}
               [:div#version "Version:"]
               [:hr]
               ;; BGM/ME
               [:div
                "BGM / ME :"
                [:br]
                (demo-button "vnctst.audio3.js.stop_bgm()"
                             "(audio3/stop-bgm!)")
                " - BGMをフェード停止させる"
                [:br]
                (demo-button "vnctst.audio3.js.stop_bgm(0)"
                             "(audio3/stop-bgm! 0)")
                " - BGMを即座に停止させる(引数はフェード秒数)"
                [:br]
                [:br]
                (demo-button "vnctst.audio3.js.play({bgm: 'drop'})"
                             "(audio3/play! :bgm/drop)")
                " - \"audio/bgm/drop.{ogg,mp3}\" をBGMとして再生する"
                [:br]
                (demo-button "vnctst.audio3.js.play({bgm: 'drop'}, 1.5, 1.2, 0.2)"
                             "(audio3/play! :bgm/drop 1.5 1.2 0.2)")
                " - \"audio/bgm/drop.{ogg,mp3}\" をBGMとして再生する。引数は音量(省略時1.0)、ピッチ(再生速度倍率、省略時1.0)、パン(左右に寄せる、省略時0、-1が左最大、1が右最大)"
                [:br]
                [:br]
                (demo-button "vnctst.audio3.js.play({me: 'unmei'})"
                             "(audio3/play! :me/unmei)")
                " - \"audio/me/unmei.{ogg,mp3}\" をMEとして再生する"
                [:br]
                (demo-button "vnctst.audio3.js.play({me: 'unmei'}, 1.5, 1.2, 0.2)"
                             "(audio3/play! :me/unmei 1.5 1.2 0.2)")
                " - \"audio/me/unmei.{ogg,mp3}\" をMEとして再生する。引数は音量(省略時1.0)、ピッチ(再生速度倍率、省略時1.0)、パン(左右に寄せる、省略時0、-1が左最大、1が右最大)"
                [:br]
                [:br]
                (demo-button "vnctst.audio3.js.play_bgm(\"audio/bgm/drop.ogg\")"
                             "(audio3/play-bgm! \"audio/bgm/drop.ogg\")")
                " - \"audio/bgm/drop.ogg\" をBGMとして再生する。任意のurlを指定可能(外部サーバを指定する場合は要CORS設定)。この環境でoggが再生可能かどうかは後述の方法で確認可能(確認せずに再生不可な環境で実行してもエラーは投げられず、何も起こらない)"
                [:br]
                (demo-button "vnctst.audio3.js.play_bgm(\"audio/bgm/drop.mp3\")"
                             "(audio3/play-bgm! \"audio/bgm/drop.mp3\")")
                " - \"audio/bgm/drop.mp3\" をBGMとして再生する"
                [:br]
                (demo-button "vnctst.audio3.js.play_me(\"audio/me/unmei.ogg\")"
                             "(audio3/play-me! \"audio/me/unmei.ogg\")")
                " - \"audio/me/unmei.ogg\" をMEとして再生する"
                ]
               [:hr]
               ;; BGS
               [:div
                "BGS :"
                [:br]
                (demo-button "vnctst.audio3.js.stop_bgs()"
                             "(audio3/stop-bgs!)")
                " - BGSをフェード停止させる"
                [:br]
                (demo-button "vnctst.audio3.js.stop_bgs(0)"
                             "(audio3/stop-bgs! 0)")
                " - BGMを即座に停止させる(引数はフェード秒数)"
                [:br]
                [:br]
                (demo-button "vnctst.audio3.js.play({bgs: 'noise'})"
                             "(audio3/play! :bgs/noise)")
                " - \"audio/bgs/noise.{ogg,mp3}\" をBGSとして再生する"
                ]
               [:hr]
               ;; SE
               [:div
                "SE :"
                [:br]
                (demo-button "vnctst.audio3.js.play({se: 'jump'})"
                             "(audio3/play! :se/jump)")
                " - \"audio/se/jump.{ogg,mp3}\" をSEとして再生する。連打での多重再生が可能"
                [:br]
                (demo-button "vnctst.audio3.js.play({se: 'yarare'})"
                             "(audio3/play! :se/yarare)")
                " - \"audio/se/yarare.{ogg,mp3}\" をSEとして再生する"
                [:br]
                [:br]
                (demo-button "vnctst.audio3.js.play_se(\"audio/se/yarare.ogg\")"
                             "(audio3/play-se! \"audio/se/yarare.ogg\")")
                " - \"audio/se/yarare.ogg\" をSEとして再生する"
                ]
               [:hr]
               ;; misc
               [:div
                "Misc :"
                [:br]
                (demo-button "vnctst.audio3.js.set_volume_master(0.25)"
                             "(audio3/set-volume-master! 0.25)")
                " - マスター音量を25%に設定する(音量値は0.0～1.0の範囲、初期値は0.5)"
                [:br]
                (demo-button "vnctst.audio3.js.set_volume_master(0.5)"
                             "(audio3/set-volume-master! 0.5)")
                " - マスター音量を50%に設定する"
                [:br]
                (demo-button "vnctst.audio3.js.set_volume_master(1.0)"
                             "(audio3/set-volume-master! 1.0)")
                " - マスター音量を100%に設定する"
                [:br]
                [:br]
                (demo-button "alert(vnctst.audio3.js.can_play_ogg())"
                             "(audio3/can-play-ogg?)")
                " - oggが再生可能なら真値を返す"
                [:br]
                (demo-button "alert(vnctst.audio3.js.can_play_mp3())"
                             "(audio3/can-play-mp3?)")
                " - mp3が再生可能なら真値を返す"
                [:br]
                (demo-button "alert(vnctst.audio3.js.can_play_m4a())"
                             "(audio3/can-play-m4a?)")
                " - m4aが再生可能なら真値を返す"
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



