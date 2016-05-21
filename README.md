(TODO: ライブラリ本体は一通り完成していますが、ドキュメントがまだ作成中です。すいません)

<div align="center"><img src="https://github.com/ayamada/vnctst-audio3/raw/master/img/logo.png" /></div>


# vnctst-audio3

[![Clojars Project](https://img.shields.io/clojars/v/jp.ne.tir/vnctst-audio3.svg)](https://clojars.org/jp.ne.tir/vnctst-audio3)

html5環境の為の、ゲーム向け音響ファイル再生ライブラリ


# 特徴

- 理解しやすい、必要最小限のインターフェース
- 曲変更に伴い自動的に適切に行われるフェードアウト/フェードイン処理
- 雑に扱える(変なrace conditionが起こらないか等に気をつかわなくてもよい)
- 再生環境に応じた、適切な再生メソッド(WebAudio, HtmlAudio, etc...)の自動選択
- RPGツクールを参考とした、 BGM, BGS, ME, SE 別の再生分類
    - BGMは、ループ再生する音源。BGMは同時に一つのみが再生可能。
    - BGSは、BGMと同時に再生可能な、ループ再生する音源。ざわめきや雨音等の環境音を(BGMとは別に)再生するような用途に使う。
    - MEは、ループ再生しない音源。BGMとは排他に再生可能。ループしないBGMや、ジングル等に使う。
        - ツクール系と違う点として、MEの再生完了時は無音となる(ツクール系は元のBGMにフェードイン戻りを行う)。
    - SEは、ループ再生しない音源。いわゆる「効果音」用。同時に多数を再生可能。
- 同一SEの多重再生への対応(これに対応していない音源ライブラリが意外と多い)
- バックグラウンド時の自動消音および復帰機能(設定により無効化可能)
- プリロード対象ファイル一覧の自動取得機能
- その他、様々なバッドノウハウ対応を内包
- cljs環境向け。しかし後述の目的の為に、js向けデプロイも一応行っている


# 試す

- TODO: あとで動作確認用の簡単なサンプルを設置する


# 使い方

TODO: js版も書く事

## 前準備

1. `project.clj` の `:dependencies` に `[jp.ne.tir/vnctst-audio3 "X.Y.Z"]` を追加する
2. 利用したい名前空間にて `(:require [vnctst.audio3 :as audio3])` みたいな感じで require しておく
3. `(audio3/init!)` を実行しておく
    - 以下のキーワード引数を与える事により、挙動をカスタマイズする事が可能
        -  `:fallback-ext "拡張子"` - プリセット音源(後述)はoggを優先しますが、oggが再生できない場合の拡張子を指定します。省略時は `"mp3"` です。ツクール準拠にしたい場合は `"m4a"` にするとよいでしょう。
        - `:url-prefix "path/to/audio/"` - プリセット音源(後述)の配置urlのprefixを指定します。省略時は `"audio/"` です。通常用途では、末尾のスラッシュは必須です。通常はこれを変更する事はありません。
        -  `:dont-stop-on-background? 真偽値` - デフォルトでは、ページのタブがバックグラウンドになった場合に自動的にBGMを一時停止します(一部、対応していないブラウザもあります)。真値を指定する事でこの挙動を無効化できます。
        -  `:always-mute-at-mobile? 真偽値` - 真値を指定する事で、`User-Agent`ヘッダがモバイル環境っぽい場合に音響再生を常時ミュート状態にします。現時点ではモバイル対応に不完全な部分があるので、真値を指定する必要があるかもしれません。
        -  `:debug? 真偽値` - 真値を指定する事で、再生動作の呼び出しや、ファイルのロードエラー等の各種情報をコンソールに出力するようになります。当ライブラリは基本的にエラーがあっても例外は投げないポリシーとしている為、エラーの検知にはこのオプションの設定が必須です。デバッグビルドでは真に、リリースビルドでは偽になるように上手く設定するとよいでしょう。
        -  `:never-use-webaudio? 真偽値` - WebAudioを常に無効化したい(つまりHtmlAudioのみ使うようにしたい)時に真値を指定します。これは基本的には当ライブラリの開発者向けの機能です。


## 最も単純な使い方

- BGMを鳴らす(停止させるまでループ再生し続ける)
    - `(audio3/play-bgm! "path/to/hoge.ogg")`

- BGMを非ループ再生する(曲の最後に到達したらそのまま再生を終了する
    - `(audio3/play-me! "path/to/fuga.ogg")`

- BGMをフェードアウトさせて(もしくは即座に)停止する
    - `(audio3/stop-bgm!)` フェードアウト1秒(デフォルト値)かけて停止
    - `(audio3/stop-bgm! 3)` フェードアウト3秒かけて停止
    - `(audio3/stop-bgm! 0)` 即座に停止
    - 既にBGMが停止状態の時は何も起こらない

- 今鳴っているBGMをフェードアウト終了させてから次の曲を再生する
    - `(audio3/play-bgm! "path/to/foo.ogg")`
        - これは上の「BGMを鳴らす」のと全く同じコードだが、これで「現在の曲をフェードアウト終了させたから次の曲を再生する」という挙動となる
        - 現在鳴らしている曲と全く同一の曲が指定された場合は何も起こらない
        - フェードアウト中に別の曲や同一曲の再生要求が行われた場合は、内部で適切に処理される事が保証される(同一曲だった場合はフェードインして元に戻る)。この辺りは内部では複雑な状態遷移を持つが、外から利用する際にそれを気にする必要はない。

- 効果音を鳴らす(複数種類もしくは同一の音源ファイルを多重再生する事が可能)
    - `(audio3/play-se! "path/to/fuga.ogg")`
    - 効果音はイベント毎に鳴らすケースが多いが、複数のイベントが同時発火して同じ効果音を同時に鳴らしてしまうと、それが重なって結果として音量の増幅が起こり、音割れを起こす事がある。これを防ぐ為に、非常に近いタイムスライス(デフォルトでは50msec以内)での同じ効果音の再生は抑制するようにしている。


## 少し複雑な使い方

- この環境にて、特定種別の音源ファイルが再生可能かを調べる
    - `(audio3/can-play-ogg?)` `=>` `true or false`
    - `(audio3/can-play-mp3?)` `=>` `true or false`
    - `(audio3/can-play-m4a?)` `=>` `true or false`
    - `(audio3/can-play? "audio/ogg")` `=>` `true or false`
    - このチェックを行わずに直に再生しようとして失敗しても別にエラーは投げられない(デバッグフラグをオンにしていればコンソールにエラー情報は出力される)ので、「どうしても再生されないと困る」ような状況でないなら、このチェックを行わなくても別に問題はない

- 音量設定を変更/取得する
    - 音量設定値は全て0.0～1.0の数値で表現される。
    - `(audio3/set-volume-master! 0.5)`
        - マスターボリュームを設定する。初期値は0.5。
        - マスターボリュームは全体に影響する音量。初期状態の場合、マスターは0.5、個別ボリュームも0.5なので、結果として、何も設定しない場合は最大音量の25%で再生される事になる。
    - `(audio3/set-volume-bgm! 0.5)`
        - BGM/BGS/MEのボリュームを設定する。初期値は0.5。
    - `(audio3/set-volume-se! 0.5)`
        - SEのボリュームを設定する。初期値は0.5。
    - `(audio3/get-volume-master)` `=>` 0.0～1.0
        - マスターボリュームを取得する
    - `(audio3/get-volume-bgm)` `=>` 0.0～1.0
        - BGM/BGS/MEのボリュームを取得する
    - `(audio3/get-volume-se)` `=>` 0.0～1.0
        - SEのボリュームを取得する

- 個別にvolume(音量), pan(左右のバランス), pitch(再生速度レート)を設定する
    - `(audio3/play-bgm! "path/to/hoge.ogg" 1.5 -1 1.2)`
    - `(audio3/play-se! "path/to/fuga.ogg" 0.15 1 0.8)`
    - volumeは0以上の小数値を指定する。ここには1.0以上の値を指定する事が可能。ただし前述のマスターボリュームおよび各ボリューム設定と乗算した結果が1.0を越える事はできない(つまり初期状態のままなら、volumeは4.0で頭打ちとなる)。省略時は 1.0 が指定されたものとして扱われる
    - panは -1.0 ～ 1.0 の小数値を指定する。省略時は 0 として扱われる
    - pitchは 0.1 ～ 10.0 の小数値を指定する。省略時は 1.0 として扱われる
    - pitchおよびpanについては、反映されない実行環境がある事に注意

- 効果音を途中で停止させる
    - `(let [ch (audio3/play-se! "path/to/fuga.ogg")] (audio3/stop-se! ch))`
    - `audio3/play-se!` は再生ごとのチャンネルを返す。これに対して `audio3/stop-se!` を行う事ができる

- BGSを再生/停止する
    - BGSとは、BGMと並行して再生できるループ音源。人ごみの音、風の音、雨音、といった環境音を、BGMと同時に再生する事を想定している
    - `(audio3/play-bgs! "path/to/foo.ogg")`
    - `(audio3/stop-bgs!)`
    - 追加の引数についてはBGMと同じ


## 本格的な使い方

- SEのプリロードを行う
    - `(audio3/preload-se! "path/to/fuga.ogg")`
    - 事前にプリロードを行う事で、初回再生時に内部でファイルのローディングをしなくてすみ、初回再生時でも遅延なく即座にSEを再生できるようになる。

- SEのプリロードが完了しているかを調査する
    - `(audio3/loaded-se? "path/to/fuga.ogg")`
        - プリロードが完了しているなら真値を返す。
        - `play-se!` も内部でプリロードを行っている為、 `preload-se!` を実行していなくても、一度鳴らしたSEは、これによって真値が返る。
        - 注意点として、ロード時に何らかのエラー(ファイルがない等)が発生したケースでも真値を返す(「エラーが起こったが、ロード自体は完了した」という扱い)。エラーを検出したい場合は後述の `succeeded-to-load-se?` を使う事。
    - `(audio3/succeeded-to-load-se? "path/to/fuga.ogg")`
        - プリロードが完了し、再生が可能な状態になっていれば真値を返す。
        - `loaded-se?` との違いはロードエラーの扱い。
        - これ単体ではロード完了待ちには使えない事に注意。ロード完了待ちをするには前述の `loaded-se?` の方が適切。
    - プリロード中に `play-se!` が実行された場合、プリロードが完了するまで再生は遅延する(通常のプリロードなし再生の時と同じ挙動)。

- SEのアンロードを行う
    - `(audio3/unload-se! "path/to/fuga.ogg")`
        - 全てのSEはプリロードもしくは再生の際に内部的にキャッシュされる。よって、「一度鳴らしたらもう二度と鳴らさない」ようなSEをどんどん大量に鳴らすようなケースでは、アンロードを行わないとメモリリークしてしまう。
        - アンロード後は、前述の `loaded-se?` と `succeeded-to-load-se?` は偽値を返すようになる。
        - アンロード後に、アンロードしたSEを再生しようとした場合、またプリロードして内部キャッシュしてからの再生となる。
        - プリロード中にアンロードを行った場合、プリロード自体が取り消される。

- BGM系のプリロードを行う
    - `(audio3/preload-bgm! "path/to/hoge.ogg")`
    - BGM/BGS/MEはどれも「BGM系」として、プリロード時には全て同じ扱いとなる
    - 注意点として、BGM系はSEとは違い、単に `play-bgm!` 等を実行しただけでは内部キャッシュ状態とはならない。BGM系は「即座に再生しないといけない」という要件があるケースがあまりなく、またメモリ消費が大きい場合が多いからである。明示的に `preload-bgm!` を実行した時のみ、プリロード状態になる。

- BGM系のプリロードが完了しているかを調査する
    - `(audio3/preloaded-bgm? "path/to/hoge.ogg")`
    - `(audio3/succeeded-to-preload-bgm? "path/to/hoge.ogg")`
    - これらについてはSEのものとほぼ同様。
    - 明示的にプリロード完了を待たなくても `play-bgm!` 系の実行は行えます(もちろんロード完了待ちは発生しますが)。

- BGM系のアンロードを行う
    - `(audio3/unload-bgm! "path/to/hoge.ogg")`
    - これもSEのものとほぼ同様。

- BGM系が再生中かどうかを調べる
    - `(audio3/playing-bgm?)`
        - BGM, MEが再生中なら真値を返します
    - `(audio3/playing-bgs?)`
        - BGSが再生中なら真値を返します


## プリセットの使い方

ここで言う「プリセット」とは、「特定ディレクトリに配置した音源ファイルを動的にプリロードし、適切な拡張子のファイルを選択して再生する」為のシステム。

1. cljsプロジェクトの `resources/public/audio/` 配下に、 `bgm/` `bgs/` `me/` `se/` というディレクトリを作ります。
2. 上記のディレクトリ内にそれぞれ、BGM, BGS, ME, SE の各音源を入れていきます。
    - 現在のhtml5の環境(各ブラウザの対応)では、単一のメディア種別(oggやmp3等)対応だけでは全ての環境をサポートする事ができません。なので、同一の内容で違うメディア種別のファイルを二つずつ用意する必要があります。
        - 例えば、後述の再生例の為には、以下のファイルを設置する必要があります。
            - `resources/public/audio/bgm/hoge.ogg`
            - `resources/public/audio/bgm/hoge.mp3`
            - `resources/public/audio/se/fuga.ogg`
            - `resources/public/audio/se/fuga.mp3`
        - この実際の動作は「oggが再生可能ならoggのファイルを再生し、oggが再生不可なら `init!` のオプションの `:fallback-ext` の値で指定された拡張子のファイル(デフォルトはmp3だが、m4aに変更も可能)で再生を行う」というものになります。
        - node-webkitやcordova等、実行環境が特定のものに固定されている場合は一つだけにする事も一応可能です。この場合は後述の「プリセットのプリロード」のオプション引数を指定してください。
3. プリセットのプリロードを行う
    - ※プリロードを行わなくても、プリセットは使えます(ただしプリロードしない場合は初回実行時にロードによる再生遅延が発生します)
    - 以下の実行により、BGM/BGS/ME/SE別に、全プリセットのプリロードが行えます
        - `(audio3/preload-all-bgm-preset!)`
        - `(audio3/preload-all-bgs-preset!)`
        - `(audio3/preload-all-me-preset!)`
        - `(audio3/preload-all-se-preset!)`
    - 以下の実行により、全プリセットのプリロードが行えます
        - `(audio3/preload-all-preset!)`
    - ただし、SE以外はプリロードしない方がいい場合が多いです。つまり通常は `(audio3/preload-all-se-preset!)` のみ実行する事を検討した方がよいでしょう。
    - もしプリセット側に何らかの不備(oggとmp3の両方が揃っていない等)がある場合、このプリセットのプリロード時にalertが表示されます。「node-webkit等を使うからoggだけの提供でよい」等の場合は、このalertを抑制する為に、これらの関数の引数として `true` を渡してください。
4. プリセットの再生を行う
    - ここまでで再生に使用してきた `play-bgm!` `play-bgs!` `play-me!` `play-se!` は、引数としてpath文字列を取っているが、代わりに `:bgm/hoge` のようなキーワードを指定する事ができる。
    - このキーワード指定の場合に限り、 `play!` を指定する事ができる。
        - `(audio3/play! :bgm/hoge)`
        - `(audio3/play! :se/fuga)`
    - 例えば `:bgm/hoge` というキーワードが指定された場合、oggの再生が可能なら `"audio/bgm/hoge.ogg"` として解釈され、 oggの再生が不可なら `"audio/bgm/hoge.mp3"` として解釈される。このfallback拡張子等は `init!` の引数によって設定変更可能。
    - 「あるMEをSEとして再生したい」というようなケースでは、明示的に `(audio3/play-se! :me/foobar)` のように呼び出す事も一応可能(しかし分かりづらい)。
5. 最初にローディング画面を出して、プリロードが完了するのを待つ
  - 以下にプリロード対象のプリセットのキー一覧が入っているので、定期的に `preloaded-bgm?` もしくは `loaded-se?` を実行し、全てのキーが真値を返せばロード完了。
      - `audio3/preset-bgm-keys`
      - `audio3/preset-bgs-keys`
      - `audio3/preset-me-keys`
      - `audio3/preset-se-keys`
  - なお、ロード完了を待たなくても通常通り再生できます。が、プリロードは「再生開始タイミングを遅延させたくない」事がメインの目的なので、通常はロード完了を待つようにした方がよいでしょう。


## その他の機能

- 内部で利用している、再生環境種別を取得する
    - `audio3/terminal-type`
        - `#{:tablet :mobile :android :ios :chrome :firefox}` のようなキーワードの入ったset
            - `:tablet` はタブレットっぽい端末全般(iPad, android, windows tablet)
            - `:mobile` は携帯っぽい端末全般(iPhone, android, windows phone)
            - `:android` はandroid全般
            - `:ios` はiOS全般
            - `:chrome` はchrome全般(PC/モバイル向けの両方含む)
            - `:firefox` はfirefox全般(PC/モバイル向けの両方含む)
        - 上記の判定は `User-Agent` ヘッダを元に判定している為、実際の再生環境とは異なる場合が普通にある。

- バックグラウンド時にも強制的にSEを鳴らす
    - `(audio3/alarm! "path/to/fuga.ogg")`
    - 名前の通り、アラーム通知用途
    - 追加引数等は `play-se!` と同様


## Cheat Sheet

```
(ns hoge.fuga
  ;; 完了待ちの説明の為に core.async を利用する
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [vnctst.audio3 :as audio3]
            [cljs.core.async :as async :refer [>! <!]]))

;; 最初にこれを実行。引数は省略可能
(audio3/init! :fallback-ext "mp3"
              :url-prefix "audio/"
              :dont-stop-on-background? false
              :always-mute-at-mobile? true
              :debug? true
              :never-use-webaudio? false
              )

;; ベース音量の設定および取得
(audio3/set-volume-master! 0.5)
(audio3/set-volume-bgm! 0.5)
(audio3/set-volume-se! 0.5)

(audio3/get-volume-master)
(audio3/get-volume-bgm)
(audio3/get-volume-se)

;; プリセットのロードを開始
(audio3/preload-all-preset!)
;; oggのみ用意する場合は引数にtrueを渡す(エラー検出抑制)
;; (audio3/preload-all-preset! true)
;; もしくは、個別に以下を実行
;; (audio3/preload-all-bgm-preset!)
;; (audio3/preload-all-bgs-preset!)
;; (audio3/preload-all-me-preset!)
;; (audio3/preload-all-se-preset!)

;; 全プリセットのプリロードの完了待ち例
(doseq [[ks bgm?] [[audio3/preset-bgm-keys true]
                   [audio3/preset-bgs-keys true]
                   [audio3/preset-me-keys true]
                   [audio3/preset-se-keys false]]]
  (let [loaded? (if bgm? audio3/preloaded-bgm? audio3/loaded-se?)
        succeeded? (if bgm?
                     audio3/succeeded-to-preload-bgm?
                     audio3/succeeded-to-load-se?)]
    (go-loop []
      (<! (async/timeout 1000))
      (if-not (empty? (remove loaded? ks))
        (recur)
        (let [errors (remove succeeded? ks)]
          (js/console.log "Completed to load all presets")
          (when-not (empty? errors)
            (js/console.log (str "But failed to load "
                                 (pr-str errors)))))))))

;; 再生
(audio3/play! :bgm/bach)
(audio3/play! :bgs/wind)
(audio3/play! :me/jingle)
(audio3/play! :se/beep)

(audio3/play-bgm! "path/to/bach.ogg")
(audio3/play-bgs! "path/to/wind.ogg")
(audio3/play-me! "path/to/jingle.ogg")
(audio3/play-se! "path/to/beep.ogg")
```

```
;; path指定の場合は、再生可能メディア種別の判定を行った方がより良い
(if (audio3/can-play-ogg?)
  (audio3/play-se! "path/to/hoge.ogg")
  (audio3/play-se! "path/to/hoge.mp3"))

(audio3/can-play-mp3?)
(audio3/can-play-m4a?)
(audio3/can-play? "audio/ogg")

;; volume, pan, pitch を指定して再生
(audio3/play! :bgm/bach 1.5 -1 1.2)

;; バックグラウンド消音を無視してSE再生
(audio3/alarm! :se/beep)

;; 停止
(audio3/stop-bgm!) ; フェードアウト1秒で停止
(audio3/stop-bgm! 3) ; フェードアウト3秒で停止
(audio3/stop-bgm! 0) ; フェードアウトなしで即座に停止
(audio3/stop-bgs!) ; BGSの停止は別扱い(BGMとMEは共通)

;; SEの停止にはSEの再生チャンネルを指定する必要がある
(go
  (let [ch (audio3/play! :se/siren)]
    (<! (async/timeout 500))
    (audio3/stop-se! ch)))

;; 個別プリロード/アンロード
(go
  (audio3/preload-bgm! "path/to/hoge.ogg")
  (audio3/preload-se! "path/to/fuga.ogg")
  (<! (async/timeout 10000))
  ;; 先にプリロードしておけば、初回再生時でもロード待ち無しに再生可能
  (audio3/play-bgm! "path/to/hoge.ogg")
  (audio3/play-se! "path/to/fuga.ogg")
  (<! (async/timeout 10000))
  ;; 今後もう再生しないなら、アンロードしてメモリを解放しておく
  (audio3/unload-bgm! "path/to/hoge.ogg")
  (audio3/unload-se! "path/to/fuga.ogg")
  )

;; その他のユーティリティ
(go-loop []
 (audio3/play! :bgm/bach)
  (<! (async/timeout 1000))
  (if (audio3/playing-bgm?)
    (recur)
    (js/console.log "Done to play BGM")))

(when-not (:mobile audio3/terminal-type)
  (audio3/play! :se/beep))
```


# 制約 / 提供しない機能 / FAQ

- 現在のところ、モバイル対応に不完全な部分がある
    - 完全な対応を行おうとする場合、 https://outcloud.blogspot.jp/2015/11/htmlaudio.html を全て満たす必要がある…
    - 将来には対応させたいところ

- RPGツクールでは、meの再生完了時には、元のbgmにフェードインして戻りますが、vnctst-audioでは、meの再生完了時はそのまま無音になる仕様です。
    - どうしてMEの仕様をツクールと違う仕様にしたのかというと、ゲーム進行とMEが同期する前提ならツクールの仕様の方が自然なのですが、非同期になる場合(BGMやMEが非常に頻繁に変化する場合)はものすごく不自然に聞こえるケースがあるので、どちらの場合でもまあまあ自然に使える現仕様としました。

- 他のライブラリで提供されているようなサウンドスプライト/オーディオスプライト機能は提供しません
    - HtmlAudio上でバックグラウンド状態にした際にタイマー精度が大幅に悪くなり切り出し判定が非常に大雑把になってしまう問題がある事、スプライト設定が煩雑になりがちな事、音源ファイルの事前結合が非常に面倒な事、個別プリロードを行う前提の場合は一つのファイルに結合するメリットが薄い事、これらを鑑みた結果として『ない方がシンプルで使いやすい』という結論になりました

- ogg, m4aの内部タグによるループ位置指定には対応しない
    - 非対応環境での対応が微妙すぎる事になる為

- 「再生完了時に指定したハンドルを実行する」機能は、標準では提供しません
    - 何故提供していないのかというと、環境によっては「再生自体行われない」ケースが普通にあり、その場合に「どのタイミングで完了ハンドルを実行するのがベストか」は一意には決まらないからです(即座実行？曲の長さだけ待って実行？)。
    - どうしてもそのような処理が必要な場合は、定期的に `playing-bgm?` を実行するcore.asyncスレッドを起動して監視してください。

- `vnctst-audio3` の「3」って何？
    - 「VNCTST gamesの内部音源ライブラリの三代目」というところから来ています。
        - 初代(`vnctst-audio`)はデバイス部に [boombox](https://github.com/CyberAgent/boombox.js) を利用していました
        - 二代目(`vnctst-audio2`)はSEのみのサポートで、初代と組み合わせて使っていました(boomboxでは同一音源の多重再生がサポートされていない問題への対処として)
    - 今回のリリースにあたって、この「3」を取って `vnctst-audio` とする事も検討したのですが、既存の自作ゲーム内にて名前空間が衝突するのが嫌なのと、今後に新しいコードベースで「4」を作る可能性がある為、敢えて「3」を残す事としました。


# html5上の音響システム固有にて、知っておくべきこと

TODO: もっと良いセクション名を考える

(「音が出ない環境の事を常に考慮する必要がある」「音が出ない環境の事を考慮し、ユーザへのレスポンスが音だけになるのは避ける。音を出すと同時にビジュアルでも何かしら示すようにする」「android上のchromeでのwebaudioのデコードが異様に重い」「一応、oggとmp3もしくはm4aで、基本的な環境での再生はカバーできるという事になっている」「違うドメインにあるファイルを再生する場合はCORS回りの設定が必要」とかの事を書く)

...


# TODO

以下のTODOは時間のある時に対応を行う予定です。

- SEを停止させる際のフェードアウト秒数指定
    - 現状では、常に0が指定された挙動になっている
    - ゲームキャラの台詞を音声再生するような用途にはMEではなくSEを使う事になるので、その際にフェードアウトしたいケースがありえるので、これは時間のある時にきちんと対応させたい

- 古いandroidのchromeでのタッチイベントアンロックが上手く動いてない
    - 試してみたところ、タッチ系ハンドル内では再生されるが、そうでない場所では再生されていない。iOS系とは違い「アンロック」という概念自体がないのか、それともインスタンス生成等の位置に影響があるのか？
    - これは可能ならきちんと対応させたい。あとで調べる。

- BGM系のプリロード関連の関数名にいまいち分かりづらい点がある
    - `audio3/preload-bgm!` `audio3/preloaded-bgm?` `audio3/succeeded-to-preload-bgm?` `audio3/unload-bgm!` は、BGM/BGS/ME共用というのが分かりづらい
    - その一方、 `audio3/playing-bgm?` と `audio3/playing-bgs?` は分かれている
    - これについては、もうちょっと良い分け方がないかどうか考えたい。しかしこれは単に「分かりづらい」というだけで、利用上の問題はないので修正するかは未定
    - 「RPGツクール風の再生分類を採用」したのが、そもそもの間違いだった可能性はある…しかし、これはこれで扱いやすいとは思うのだが

以下のTODOは基本的に達成されません。pull-req等を受け付ける為の項目です。

- 英語版ドキュメントの作成
    - 誰か他の人にやってもらう(@ayamadaには不要な為)

- js/ts版の作成
    - 誰か他の人にやってもらう(@ayamadaには不要な為)

- プリロード回りの抽象化が不完全(ベタ書きになっている)なのを直す
    - とりあえず現状で動いているので、今は直さない方針とする

- MEが終了直前のタイミングで別の曲をplayして、フェードアウト中にMEが終了した場合、フェードアウトにかかる残り秒数だけ、次の曲の再生が待たされる
    - そんなに致命的な問題ではないので、もしこれが問題になるケースが出てきた段階に改めて対応する


# Specifications

(TODO: urlを修正)

[SPEC.md](SPEC.md) にて、 `vnctst-audio3` の内部仕様書を公開しています。

同様のライブラリを作りたい方は参考にできるかもしれません。


# Development

(TODO: urlを修正)

`vnctst-audio3` 自体の開発手順については [DEVEL.md](DEVEL.md) を参照してください。


# License

MIT

(TODO: あとでちゃんと書く事)


# ChangeLog

- 0.1.0 (2016-XX-XX 現在準備中)
    - Initial release

- 0.1.0-rc1 (2016-05-21)
    - 京都Clojureの為に、プレリリース版を作成
    - ドキュメントがまだ作成途中のまま

- X.X.X (2016-04-XX 非公開)
    - コア部分は完成
    - ドキュメントなし


