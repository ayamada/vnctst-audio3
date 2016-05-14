(ns vnctst.audio3.device.entry-table
  (:refer-clojure :exclude [get]))

(defonce entry-table (atom {}))



;;; API設計メモ
;;; - audio-source および audio-channel は、明示的にdisposeする仕様とする。
;;;   :web-audio も :html-audio も、明示的にdisposeする必要はないのだが、
;;;   プールを実装する際にdisposeが必要となる。
;;; - 再生対象ファイルの種別は、基本的に ogg とする。
;;;   oggの再生ができない一部の環境では、
;;;   ogg の代わりに mp3 もしくは m4a を渡してもよい。
;;;   これは :can-play-ogg? によって判定される。




(def device-fn-keywords
  #{:init!?
    ;; ((:init!? device))
    ;; このデバイスが、この環境にて利用可能かどうかを返す。
    ;; (例えば、WebAudioそのものをサポートしているか等)
    ;; また、最初に実行される事が確定しているので、
    ;; 真値を返す場合はそこで初期化も行うとよい
    :load-audio-source!
    ;; ((:load-audio-source! device) url loaded-handle error-handle)
    ;; 指定されたurlのロードを開始し、ロードが正常に完了したら
    ;; loaded-handle にロードした結果のaudio-sourceを渡して実行する。
    ;; 何らかのエラーが発生したら error-handle にエラー内容を渡して実行する。
    ;; loaded-handle類は即座に実行しても問題ないし、後で非同期実行してもよい
    ;; (この関数自体の返り値は特に利用されない)
    ;; audio-source自体は {:type :audio-source ...} のようなmapとする。
    ;; (ロードの完了したaudio-sourceは基本的にimmutableなものとして扱うので、
    ;; mapとしている。もしmutableな状態を扱う必要がある場合は、子エントリとして
    ;; atom等で保持する事)
    :dispose-audio-source!
    ;; ((:dispose-audio-source! device) audio-source)
    ;; この audio-source を完全に破棄する(GC可能になる)
    ;; 論理層により、これが実行されるのはplay!中でない事が保証されている
    ;; 既にGC可能等で、破棄する必要がない場合は何もしなくてよい
    :spawn-audio-channel
    ;; ((:spawn-audio-channel device) audio-source)
    ;; ロードされた audio-source から audio-channel を生成する。
    ;; 一つの audio-source から複数の audio-channel を生成できるものとする。
    ;; audio-channel 自体は (atom {:type :audio-channel ...}) のatomとする。
    ;; (audio-sourceとは違い、変化する状態を持つ必要があるのでatomとした)
    :pos
    ;; ((:pos device) ch)
    ;; このaudio-channelが、再生して何秒(sec)のところかを返す。
    ;; 再生開始前は0を、一度でもplay!してからstop!した場合はstop!時の値を返す。
    ;; (何らかの要因で一時的に再生停止した後に、
    ;; またそのポジションから再生し直す用途を想定した機能)
    :play!
    ;; ((:play! device) ch start-pos loop? volume pitch pan alarm?)
    ;; 指定したポイントから再生を行う。
    ;; volumeは0.0～1.0の値。
    ;; pitchは1.0基準の値。
    ;; panは-1.0～1.0の値、もしくは三要素のseq。
    :playing?
    ;; ((:playing? device) ch)
    ;; 再生中かどうかを返す。
    ;; ループ指定がない場合、曲の最後に到達したら自動的に停止する必要がある。
    ;; (つまりこれが偽値を返すように、内部状態を変更する必要がある)
    :stop!
    ;; ((:stop! device) ch)
    ;; 再生を停止する(デバイス側ではフェード等を考えなくてよい)
    ;; 再生停止後にposを取ると、停止したタイミングのposが取れるものとする
    :set-volume!
    ;; ((:set-volume! device) ch new-value)
    ;; volumeを変更する
    :set-pitch!
    ;; ((:set-pitch! device) ch new-value)
    ;; pitchを変更する
    :set-pan!
    ;; ((:set-pan! device) ch new-value)
    ;; panを変更する
    :dispose-audio-channel!
    ;; ((:dispose-audio-channel! device) ch)
    ;; このaudio-channelを完全に破棄する(GC可能になる)
    ;; bgm等はchを保持したくないので、これを利用する
    ;; 論理層により、これが実行されるのはplay!中でない事が保証されている
    ;; 既にGC可能等で、破棄する必要がない場合は何もしなくてよい
    })

(defn register! [k m]
  (doseq [target-k device-fn-keywords]
    (assert (clojure.core/get m target-k)))
  (swap! entry-table assoc k m))




(defn get [k]
  (clojure.core/get @entry-table k))









