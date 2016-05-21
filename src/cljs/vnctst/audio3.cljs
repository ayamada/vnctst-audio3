(ns vnctst.audio3
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [vnctst.audio3.state :as state]
            [vnctst.audio3.util :as util]
            [vnctst.audio3.common :as common]
            [vnctst.audio3.se :as se]
            [vnctst.audio3.bgm :as bgm]
            [vnctst.audio3.preset :as preset]
            [cljs.core.async :as async :refer [>! <!]]
            ))


;;; 最初に必ずこれを実行する事。
;;; なお何回も実行しても問題はないが、二回目以降のoptionsは反映されない。
(defn init! [& options]
  (apply common/init! options)
  (apply se/init! options)
  (apply bgm/init! options)
  nil)



;;; 基本音量設定へのアクセサ
;;; - 上記の init! の実行時に初期化される。
;;;   なので set-*! 系を実行する前に init! を済ませておく事。
;;; - 0～100の値を設定する事(つまり%指定)。初期値は全て50%。
;;; - ここで設定されるのは基本音量。実際の再生時の音量は、
;;;   「マスター音量」×「各BGM/BGS/ME/SEの設定音量」×「play!の引数指定音量」
;;;   となる。
;;;   - つまり、デフォルトの状態でSEを鳴らすと、50% * 50% * 100% の、
;;;     元々のoggファイルの25%の音量で鳴る事になる。
;;;   - 前述の「play!の引数指定音量」のみ、100%以上の指定を行う事が可能。
;;;     ただし元々のoggファイルの音量の100%を越える事はできない。

(defn get-volume-master [] (state/get :volume-master))
(defn get-volume-bgm [] (state/get :volume-bgm))
(defn get-volume-bgs [] (state/get :volume-bgs))
(defn get-volume-me [] (state/get :volume-me))
(defn get-volume-se [] (state/get :volume-se))

(defn set-volume-master! [v]
  (state/set! :volume-master (max 0 (min 1 v)))
  (bgm/sync-bgm-volume!)
  (bgm/sync-bgs-volume!))
(defn set-volume-bgm! [v]
  (state/set! :volume-bgm (max 0 (min 1 v)))
  (bgm/sync-bgm-volume!))
(defn set-volume-bgs! [v]
  (state/set! :volume-bgs (max 0 (min 1 v)))
  (bgm/sync-bgs-volume!))
(defn set-volume-me! [v]
  (state/set! :volume-me (max 0 (min 1 v)))
  (bgm/sync-bgm-volume!))
(defn set-volume-se! [v]
  (state/set! :volume-se (max 0 (min 1 v)))
  ;; NB: SEのみ、現チャンネルへの反映は不要
  )



;;; stop系はデフォルトでフェードアウト終了する。
;;; 引数に数値を指定する事で、フェードアウト秒数を変更可能。
;;; 引数に0を設定する事により、即座に停止させられる。

(defn stop-bgm! [& [fade-sec]] (bgm/stop-bgm! fade-sec))

(defn stop-bgs! [& [fade-sec]] (bgm/stop-bgs! fade-sec))

(defn stop-me! [& [fade-sec]] (bgm/stop-me! fade-sec))

;;; NB: これのみ特殊で、play-se!の返り値を引数に渡す必要がある。
;;;     これによって、特定のSEのみを停止させる事ができる。
;;; TODO: seのfade-sec対応はまだ未実装
(defn stop-se! [chan & [fade-sec]] (se/stop! chan fade-sec))



;;; NB: play系の第一引数は、keywordもしくは実path文字列のどちらか。
;;;     keywordの場合はoggもしくはfallbackのmp3/m4aを自動的に選択されるが、
;;;     文字列指定の場合はこの自動選択が機能しないので、
;;;     必要であれば後述の can-play? 系を使って、
;;;     再生可能かどうかを事前に呼出元にてチェックする事。

(defn play-bgm! [key-or-path & [vol pitch pan]]
  (bgm/play-bgm! key-or-path vol pitch pan))

(defn play-bgs! [key-or-path & [vol pitch pan]]
  (bgm/play-bgs! key-or-path vol pitch pan))

(defn play-me! [key-or-path & [vol pitch pan]]
  (bgm/play-me! key-or-path vol pitch pan))

;;; 返り値として、 stop-se! に渡す為の引数もしくはnilが返される。
;;; nilが返った時は、何らかの原因で再生が抑制された事を意味する。
;;; (同一SEの一定秒数内での連打抑制機能が付いているので、それに引っかかった等)
(defn play-se! [key-or-path & [vol pitch pan]]
  (se/play! key-or-path vol pitch pan))

;;; 基本的には play-se! と同じだが、こちらはバックグラウンド中でも再生が可能。
;;; 「バックグラウンドだけど、ユーザに通知したい」時の為の機能。
(defn alarm! [key-or-path & [vol pitch pan]]
  (se/alarm! key-or-path vol pitch pan))

;;; keywordのnamespaceから自動判別して再生する。通常はこれを使えばよい
(defn play! [k & args]
  (assert (keyword? k)) ; 文字列指定不可
  (case (namespace k)
    "bgm" (apply play-bgm! k args)
    "bgs" (apply play-bgs! k args)
    "me" (apply play-me! k args)
    "se" (apply play-se! k args)
    (throw (ex-info "Invalid keyword"
                    {:args (list* 'play! k args)}))))

(defn playing-bgm? [] (bgm/playing-bgm?))
(defn playing-bgs? [] (bgm/playing-bgs?))
(defn playing-me? [] (bgm/playing-me?))


;;; BGM/BGS/ME専用ユーティリティ

;;; 指定したBGM/BGS/MEを即座に再生できるように、プリロードを開始しておく。
;;; 既にプリロードが済んでいる場合は何も起こらない。
;;; (プリロードしなくても再生は可能だが、ロードが完了するまで待たされる)
(defn preload-bgm! [key-or-path] (bgm/preload! key-or-path))
(def preload-bgs! preload-bgm!)
(def preload-me! preload-bgm!)

;;; プリロード済のBGM/BGS/MEをキャッシュテーブルから解放する。
(defn unload-bgm! [key-or-path] (bgm/unload! key-or-path))
(def unload-bgs! unload-bgm!)
(def unload-me! unload-bgm!)

;;; プリロードの状態を調べる関数。プリロードが完了している場合のみ真値を返す。
;;; NB: エラー発生時も「プリロード自体は完了」として真値を返す。
;;;     プリロードが成功したかどうかを見るには
;;;     (succeeded-to-preload-bgm? k) を呼ぶ事。
;;;     また unload-bgm! されると偽値に戻る点にも注意。
(defn preloaded-bgm? [key-or-path]
  (bgm/preloaded? key-or-path))

;;; プリロード済かつプリロード成功であれば真値を返す
(defn succeeded-to-preload-bgm? [key-or-path]
  (bgm/succeeded-to-preload? key-or-path))




;;; SE専用ユーティリティ
;;; SEは「即座に鳴る」事が重視されるので、
;;; 事前にプリロードしておき、キャッシュテーブルに登録する事ができる。
;;; (プリロードなしでも再生可能だが、初回再生時にはロード待ちが発生する)

;;; 指定したSEを即座に再生できるように、事前ロードを開始しておく。
;;; 既に事前ロードが済んでいる場合は何も起こらない。
(defn preload-se! [key-or-path] (se/preload! key-or-path))

;;; ロード済のSEをキャッシュテーブルから解放する。
;;; 一時的にしか使わないSEが大量にある場合等に使う。
;;; これで解放しても、同じSEをまた再生しようとする事は可能。
;;; (もちろんロード待ちが発生する)
;;; NB: 対応するSEがまだ鳴っている最中に実行しない事。
(defn unload-se! [key-or-path] (se/unload! key-or-path))

;;; プリロードの状態を調べる関数。ロードが完了している場合のみ真値を返す。
;;; NB: エラー発生時も「ロード自体は完了」として真値を返す。
;;;     ロードが成功したかどうかを見るには(succeeded-to-load-se? k)を呼ぶ事。
;;;     また unload-se! されると偽値に戻る点にも注意。
(defn loaded-se? [key-or-path]
  (se/loaded? key-or-path))

;;; ロード済かつロード成功であれば真値を返す
(defn succeeded-to-load-se? [key-or-path]
  (se/succeeded-to-load? key-or-path))


;;; TODO: se/same-se-interval を外側からいじるインターフェースを提供する事





;;; その他のユーティリティ

;;; 再生環境種別の入ったset。今のところ、以下の各値が入る
;;; #{:tablet :mobile :android :ios :chrome :firefox}
(def terminal-type util/terminal-type)

;;; 指定したmimeの音源ファイルが再生可能かを返す。
;;; "audio/ogg" のような値を指定する。
(defn can-play? [mime] (util/can-play? mime))

;;; can-play? の、特定拡張子に特化したバージョン
(defn can-play-ogg? [] (util/can-play-ogg?))
(defn can-play-mp3? [] (util/can-play-mp3?))
(defn can-play-m4a? [] (util/can-play-m4a?))

;;; 0.0～1.0 の値と 0～100 のパーセント値を相互変換する。ボリューム値用。
(def float->percent util/float->percent)
(def percent->float util/percent->float)





;;; プリセットをプリロードする
(defn preload-all-bgm-preset! [& [silent?]]
  (preset/preload-all-bgm-preset! silent?))
(defn preload-all-bgs-preset! [& [silent?]]
  (preset/preload-all-bgs-preset! silent?))
(defn preload-all-me-preset! [& [silent?]]
  (preset/preload-all-me-preset! silent?))
(defn preload-all-se-preset! [& [silent?]]
  (preset/preload-all-se-preset! silent?))

(defn preload-all-preset! [& [silent?]]
  (preset/preload-all-bgm-preset! silent?)
  (preset/preload-all-bgs-preset! silent?)
  (preset/preload-all-me-preset! silent?)
  (preset/preload-all-se-preset! silent?))

;;; プリセット定義の一覧を取得する(初回ロード待ち判定等に使う想定)
(def preset-bgm-keys preset/all-bgm-keys)
(def preset-bgs-keys preset/all-bgs-keys)
(def preset-me-keys preset/all-me-keys)
(def preset-se-keys preset/all-se-keys)



