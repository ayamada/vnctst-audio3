(ns vnctst.audio3.device
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [vnctst.audio3.state :as state]
            [vnctst.audio3.util :as util]
            [vnctst.audio3.device.entry-table :as entry-table]
            [vnctst.audio3.device.dumb]
            [vnctst.audio3.device.web-audio]
            [vnctst.audio3.device.html-audio-multi]
            [vnctst.audio3.device.html-audio-single]
            [cljs.core.async :as async :refer [>! <!]]
            ))

;;; TODO: init! を実行する前にサードパーティー製のデバイスを設定する事で、
;;;       そのデバイスを優先して利用できる仕組みを組み込みたい





;;; :init!? を一回だけ実行する事を保証する為に、実行結果をキャッシングする
(defonce init-device-table (atom {}))
(defn init-device!? [device-key]
  (if (contains? @init-device-table device-key)
    (get @init-device-table device-key)
    (let [device (entry-table/get device-key)
          ok? ((get device :init!?))]
      (swap! init-device-table device-key (if ok? device false)))))







;;; TODO: 一部の古いモバイル系は :dumb 固定にしたい。しかしどう判定する？

(defn- determine-se-device-keys [never-use-webaudio? never-use-htmlaudio?]
  (let [r (if (or
                (:android util/terminal-type)
                (:ios util/terminal-type))
            [:web-audio :html-audio-single :dumb]
            [:web-audio :html-audio-multi :dumb])
        r (if never-use-webaudio?
            (vec (remove #{:web-audio} r))
            r)
        r (if never-use-htmlaudio?
            (vec (remove #{:html-audio-single :html-audio-multi} r))
            r)]
    r))

(defn- determine-bgm-bgs-me-device-keys
  [never-use-webaudio? never-use-htmlaudio?]
  (if never-use-htmlaudio?
    [:web-audio :dumb]
    ;; androidのchromeではWebAudioのデコードに時間がかかるらしいので、
    ;; これのみ特別扱いして、WebAudioを使わせないようにする
    (if (and
          (:android util/terminal-type)
          (:chrome util/terminal-type))
      [:html-audio-single :dumb]
      ;; そうでなければ、選択基準はSEと同じでよい
      (determine-se-device-keys never-use-webaudio? never-use-htmlaudio?))))


(defn- resolve-device [device-keys]
  (loop [device-keys device-keys]
    (when-let [k (first device-keys)]
      (if-let [device (init-device!? k)]
        device
        (recur (rest device-keys))))))


;;; 適切にデバイスの判定と初期化を行い、stateに保存する
(defn init! [& [never-use-webaudio? never-use-htmlaudio?]]
  (let [se-device (resolve-device (determine-se-device-keys never-use-webaudio? never-use-htmlaudio?))
        bgm-bgs-me-device (resolve-device (determine-bgm-bgs-me-device-keys never-use-webaudio? never-use-htmlaudio?))]
    (assert se-device)
    (assert bgm-bgs-me-device)
    (state/set! :se-device se-device)
    (state/set! :bgm-bgs-me-device bgm-bgs-me-device)
    true))





(defn- check-device-fn-keyword! [k]
  (assert (get entry-table/device-fn-keywords k)))

;;; デバイス関数を実行する

(defn se-call! [k & args]
  (check-device-fn-keyword! k)
  (apply (get (state/get :se-device) k) args))

(defn bgm-call! [k & args]
  (check-device-fn-keyword! k)
  (apply (get (state/get :bgm-bgs-me-device) k) args))


