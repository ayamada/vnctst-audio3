(ns vnctst.audio3.util
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [vnctst.audio3.state :as state]
            [cljs.core.async :as async :refer [>! <!]]
            ))


;;; percentage utilities

(defn float->percent [f]
  (when f
    (js/Math.round (* 100 f))))

(defn percent->float [percent]
  (when percent
    (/ percent 100)))





;;; https://w3g.jp/blog/js_browser_sniffing2015
(defn- detect-terminal-type-by-user-agent []
  (let [ua (js/window.navigator.userAgent.toLowerCase)
        has-not? #(neg? (.indexOf ua %))
        has? (complement has-not?)
        tablet? (or
                  (and (has? "windows") (has? "touch") (has-not? "tablet pc"))
                  (and (has? "ipad"))
                  (and (has? "android") (has-not? "mobile"))
                  (and (has? "firefox") (has? "tablet"))
                  (and (has? "kindle"))
                  (and (has? "silk"))
                  (and (has? "playbook"))
                  )
        mobile? (or
                  (and (has? "windows") (has? "phone"))
                  (and (has? "iphone"))
                  (and (has? "ipod"))
                  (and (has? "android") (has? "mobile"))
                  (and (has? "firefox") (has? "mobile"))
                  (and (has? "blackberry"))
                  )
        android? (or
                   (has? "android")
                   (has? "kindle")
                   (has? "silk"))
        ios? (or
               (has? "iphone")
               (has? "ipod")
               (has? "ipad"))
        chrome? (has? "chrome")
        firefox? (has? "firefox")]
    (into #{} (filter identity
                      [(and tablet? :tablet)
                       (and mobile? :mobile)
                       (and android? :android)
                       (and ios? :ios)
                       (and chrome? :chrome)
                       (and firefox? :firefox)
                       ]))))

(defonce terminal-type (detect-terminal-type-by-user-agent))




;;; :debug? フラグに関わらず、コンソールにログ出力する
(defn logging-force [& msgs]
  (when-let [c (aget js/window "console")]
    (when (aget c "log")
      (.log c (apply pr-str msgs)))))

;;; :debug? フラグがオンの時のみ、コンソールにログ出力する
(defn logging [& msgs]
  (when (state/get :debug?)
    (apply logging-force msgs)))







;;; iOSでは、タッチイベントをトリガーにして再生を行う事による
;;; アンロック処理が必要となる
;;; unlock-fnは、アンロックに成功したら真を返す事。
;;; (偽値を返す事で、次にまたリトライを行う)
;;; アンロックは一回行えば、それ以降は行わなくてもよい(らしい)
(defn register-ios-unlock-fn! [unlock-fn]
  (let [event-name "touchstart"
        h (atom nil)]
    (reset! h (fn [e]
                (when (unlock-fn)
                  (js/document.removeEventListener event-name @h))))
    (js/document.addEventListener event-name @h)))








(def can-play?
  (memoize
    (fn [mime]
      ;; NB: ここで new Audio() 相当を実行しているが、これはiOSにて問題が出る
      ;;     場合があるらしい
      ;;     http://qiita.com/gonshi_com/items/e41dbb80f5eb4c176108
      ;;     適切に回避する方法があれば回避したいところだが、よく分からない
      (let [audio-class (or
                          (aget js/window "Audio")
                          (aget js/window "webkitAudio"))
            audio (when audio-class
                    (try
                      (new audio-class)
                      (catch :default e
                        nil)))]
        (when audio
          (not (empty? (.canPlayType audio mime))))))))

(defn can-play-ogg? [] (can-play? "audio/ogg"))
(defn can-play-mp3? [] (can-play? "audio/mpeg"))
(defn can-play-m4a? [] (can-play? "audio/mp4"))






(defn key->path [key-or-path]
  (if (string? key-or-path)
    key-or-path
    (str (state/get :url-prefix)
         (namespace key-or-path)
         "/"
         (name key-or-path)
         "."
         (state/get :default-ext))))






;;; プリロードスケジューラ
(defn run-preload-process! [proc queue resolver]
  (when-not @proc
    (reset! proc true)
    (go-loop []
      (if (empty? @queue)
        (reset! proc nil)
        (let [one (first @queue)]
          (swap! queue rest)
          (<! (resolver one))
          (recur))))))




;;; パラメータ計算
(defn calc-internal-params [mode vol & [pitch pan]]
  (let [volume-key ({:bgm :volume-bgm
                     :se :volume-se} mode)
        _ (assert volume-key (str "Invalid mode " mode))
        i-vol (max 0 (min 1 (* (or vol 1)
                               (state/get :volume-master 0.5)
                               (state/get volume-key 0.5))))
        i-pitch (max 0.1 (min 10 (or pitch 1)))
        i-pan (max -1 (min 1 (or pan 0)))]
    [i-vol i-pitch i-pan]))




