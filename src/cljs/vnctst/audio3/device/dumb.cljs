(ns vnctst.audio3.device.dumb
  (:require [vnctst.audio3.device.entry-table :as entry-table]
            [vnctst.audio3.util :as util]))

(defn- p [& args]
  (util/logging :dumb args))




(defn init!? []
  (p 'init!?)
  true)

(defn load-audio-source! [url loaded-handle error-handle]
  (p 'load-audio-source! url)
  (let [audio-source {:type :audio-source
                      :url url}]
    (loaded-handle audio-source)))

(defn dispose-audio-source! [audio-source]
  (p 'dispose-audio-source! audio-source)
  nil)

(defn spawn-audio-channel [audio-source]
  (p 'spawn-audio-channel audio-source)
  (atom {:type :audio-channel
         :audio-source audio-source
         :vol 1
         :pitch nil
         :pan 0.5
         :play-start-msec 0
         :play-stop-msec 0
         :end-pos nil
         }))








(defn pos [ch]
  (p 'pos ch)
  (let [play-start-msec (or (:play-start-msec @ch) 0)
        play-stop-msec (or (:play-stop-msec @ch) (js/Date.now))]
    (* 0.001 (- play-stop-msec play-start-msec))))

(defn play! [ch start-pos loop? volume pitch pan alarm?]
  (p 'play! ch start-pos loop? volume pitch pan alarm?)
  (swap! ch merge {:vol volume
                   :pitch pitch
                   :pan pan
                   :loop loop?
                   :start-pos start-pos
                   :play-start-msec (- (js/Date.now) start-pos)
                   :play-stop-msec nil
                   :alarm? alarm?
                   })
  ch)

(defn playing? [ch]
  false)

(defn stop! [ch]
  (p 'stop! ch)
  (swap! ch assoc :play-stop-msec (js/Date.now))
  ch)

(defn set-volume! [ch volume]
  (p 'set-volume! ch volume)
  (swap! ch assoc :vol volume)
  nil)

(defn set-pitch! [ch pitch]
  (p 'set-pitch! ch pitch)
  (swap! ch assoc :pitch (merge (:pitch @ch) pitch))
  nil)

(defn set-pan! [ch pan]
  (p 'set-pan! ch pan)
  (swap! ch assoc :pan (merge (:pan @ch) pan))
  nil)

(defn dispose-audio-channel! [ch]
  (p 'dispose-audio-channel! ch)
  nil)


(entry-table/register!
  :dumb
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


