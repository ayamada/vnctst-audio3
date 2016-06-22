(ns vnctst.audio3.device.html-audio-multi
  (:require [vnctst.audio3.device.entry-table :as entry-table]
            [vnctst.audio3.device.html-audio-single :as html-audio-single]
            [vnctst.audio3.util :as util]))

(defn- apply-single-fn [f & args]
  (binding [html-audio-single/p-key :html-audio-multi]
    (apply f args)))


;;; 実装について
;;; - html-audio-single をラッピングしたもの
;;; - html-audio-single の audio-source を拡張してプールスロットを確保する
;;; - audio-channel を生成する際には、このプールから再利用する。
;;;   プールが空の場合は新規生成する。
;;;   一旦増えたプールを減らす処理は特に入れない事にする。
;;; - audio-channel のdispose時にプールに戻す。
;;; - audio-source のdispose時にプール全体を破棄する。




(defn init!? []
  (apply-single-fn html-audio-single/init!?))





(defn load-audio-source! [url loaded-handle error-handle]
  (let [extended-loaded-handle (fn [as]
                                 (let [a (:audio as)
                                       r (assoc as :pool (atom #{a}))]
                                   (loaded-handle r)))]
    (apply-single-fn html-audio-single/load-audio-source!
                     url extended-loaded-handle error-handle)))





(defn dispose-audio-source! [audio-source]
  ;; 一応、プールを明示的に破棄しておく
  (reset! (:pool audio-source) #{})
  (apply-single-fn html-audio-single/dispose-audio-source! audio-source))





(defn- resolve-audio! [audio-source]
  ;; エラー時は再生できないので、処理を行わない(nilとする)
  (when-not @(:error? audio-source)
    ;; :poolから取れるなら取るが、なければ新規に生成する
    (if-let [audio (first @(:pool audio-source))]
      ;; :pool から取れるので、それを使う
      (do
        (swap! (:pool audio-source) disj audio)
        audio)
      ;; :pool が空なので新たに生成する。
      ;; ロードは即座に完了しないので遅延が発生するが、これは許容する。
      ;; (詳細については html-audio-single の play! 内のコメント参照)
      ;; html-audio-single/_load-audio-source! でのロードは前述の通り遅延するが
      ;; audio-source インスタンス自体はすぐに返り値として得られるので、
      ;; そこから :audio 要素だけと取って返せばよい。
      ;; ここでエラーが起こらない事は一応保証されている。
      (:audio (html-audio-single/_load-audio-source!
                (:url audio-source) identity identity)))))

(defn spawn-audio-channel [audio-source]
  (let [ac-single (apply-single-fn html-audio-single/spawn-audio-channel
                                   audio-source)
        a (resolve-audio! audio-source)]
    ;; :audio だけ差し替えればそれで機能する
    (swap! ac-single assoc :audio a)
    ac-single))




(defn pos [ch]
  (apply-single-fn html-audio-single/pos ch))


(defn play! [ch start-pos loop? volume pitch pan alarm?]
  (apply-single-fn
    html-audio-single/play! ch start-pos loop? volume pitch pan alarm?))



(defn playing? [ch]
  (apply-single-fn html-audio-single/playing? ch))

(defn stop! [ch]
  (apply-single-fn html-audio-single/stop! ch))

(defn set-volume! [ch volume]
  (apply-single-fn html-audio-single/set-volume! ch volume))

(defn set-pitch! [ch pitch]
  (apply-single-fn html-audio-single/set-pitch! ch pitch))

(defn set-pan! [ch pan]
  (apply-single-fn html-audio-single/set-pan! ch pan))

(defn dispose-audio-channel! [ch]
  ;; 使い終わったchをpoolに戻す
  (let [as (:audio-source @ch)]
    (swap! (:pool as) conj (:audio @ch))))




(entry-table/register!
  :html-audio-multi
  {:init!? init!?
   :load-audio-source! load-audio-source!
   :dispose-audio-source! dispose-audio-source!
   :spawn-audio-channel spawn-audio-channel
   :pos pos
   :play! play!
   :playing? playing?
   :stop! stop!
   :set-volume! set-volume!
   :set-pitch! set-pitch!
   :set-pan! set-pan!
   :dispose-audio-channel! dispose-audio-channel!
   })


