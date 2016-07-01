<div align="center"><img src="https://github.com/ayamada/vnctst-audio3/raw/master/img/logo.png" /></div>


# vnctst-audio3

[![release version](https://img.shields.io/github/release/ayamada/vnctst-audio3.svg)](https://github.com/ayamada/vnctst-audio3/releases)
[![Clojars Project](https://img.shields.io/clojars/v/jp.ne.tir/vnctst-audio3.svg)](https://clojars.org/jp.ne.tir/vnctst-audio3)
[![Build Status](https://travis-ci.org/ayamada/vnctst-audio3.svg?branch=master)](https://travis-ci.org/ayamada/vnctst-audio3)
[![license zlib](https://img.shields.io/badge/license-zlib-blue.svg)](LICENSE)

html5環境の為の、ゲーム向け音響ファイル再生ライブラリ


# 目次

- [前書き](#前書き)
- [特徴](#特徴)
- [オンラインデモ](#オンラインデモ)
- [cljs版の使い方](#cljs版の使い方)
- [js版の使い方](#js版の使い方)
- [制約 / 提供しない機能 / FAQ](#制約--提供しない機能--faq)
- [必須知識](#必須知識)
- [TODO](#todo)
- [Specifications](#specifications)
- [Development](#development)
- [License](#license)
- [ChangeLog](#changelog)


# 前書き

音響ファイルを再生するjsライブラリは既存のものが多数あるものの、「ゲーム向け」としてはどれも後述の機能が足りていなかった。だから、この`vnctst-audio3`を作った。

「ゲーム向け音響ファイル再生ライブラリ」として必要とされる要件は以下の通り。

- SE系の管理とBGM系の管理が分かれている事(それぞれで求められる要件がかなり違う)
    - SE系
        - 同一SEの多重再生への対応
            - 違うSEの多重再生には対応していても、同一SEの多重再生に対応していないライブラリが意外と多い
            - ゲームでは弾丸の射出音や爆発音など、同一のSEがタイミングをずらして多重に再生される事になるケースが多い
    - BGM系
        - 曲変更に伴い自動的かつ適切に行われるフェードアウト/フェードイン処理
            - フェード自体をサポートしていないライブラリが多く、またフェードをサポートしていても扱いが不完全だったりするものしかない
                - 次の曲を再生する時に、現在の曲をフェードアウトしてから再生するようにキューイングしてほしい。同時に、このフェードアウトの途中でのキャンセル処理/更に違う曲への差し替えもサポートしてほしい
                - フェードのキャンセル処理は、フェードアウト中ならフェードインして戻る、フェードイン中ならフェードアウトして戻る処理が望ましい

- 雑に扱える事
    - BGMやSEを再生/変更/停止する操作は、ゲームでは頻出の操作であり、息をするように手軽にできなくてはならない。
    - 前述のフェードアウトの件にある通り、既に再生中のBGMを変更しようとした際に「フェードアウト→次の曲を再生」という処理にしたい事はよくある。しかしこの処理は非同期となり、「フェードアウト中にまた別の曲を再生しようとしたらどうなるか」「このフェードアウト自体をキャンセルしてフェードイン復帰したくなったらどうするか」等に対応させようとすると非常に複雑な状態遷移を持つ。この状態遷移を気にせずに気軽に再生/変更/停止が行えなくてはならない。


# 特徴

- 上記の「ゲーム向け」の要件を一通り満たしている

- 憶える量の少ない、必要最小限のインターフェース
    - 前述の「複雑な状態遷移」を内部に隠しつつ、基本となるインターフェースを「再生」「停止」関連に絞り、シンプルに扱えるようにした

- 再生環境に応じた、適切な再生メソッド(WebAudio, HtmlAudio, etc...)の自動選択

- RPGツクールを参考とした、 BGM, ME, BGS, SE 別の再生分類
    - BGMは、ループ再生する音源。BGMは同時に一つのみが再生可能。
    - MEは、ループ再生されない音源。再生時に現在再生中のBGMを停止する(つまりBGMと排他)。ジングル等に使う。
        - システム内部では「BGMの一種」として扱われる(ボリューム設定等が共通)。
        - ツクール系での仕様と違う点として、MEの再生完了時は無音となる(ツクール系では元のBGMへとフェードイン戻りを行う)。
    - BGSは、BGMと同時に再生可能な、ループ再生する音源。ざわめきや雨音等の環境音を(BGMと同時に)再生するような用途に使う。
        - システム内部では「BGMの一種」として扱われる(ボリューム設定等が共通)。
    - SEは、ループ再生しない音源。いわゆる「効果音」用。同時に多数を再生可能。

- バックグラウンド時の自動消音および復帰機能(設定により無効化可能)

- プリロード対象ファイル一覧の自動取得機能

- html5環境特有の様々なバッドノウハウ対応を内包
    - ※ただし現状では一部のモバイル端末への対応が不完全

- cljs環境向け。しかし実験的にjs向けデプロイも行っている

- ライセンスに[zlib](https://ja.wikipedia.org/wiki/Zlib_License)風ライセンスを採用。ライセンス条文等を表示したりしなくてよい


# オンラインデモ

- 動作確認デモページ http://vnctst.tir.jp/vnctst-audio3-demo/
    - BGM変更のフェードアウトおよび同じ曲に戻る際のフェードイン辺りが目玉です。雑に扱っても、この辺りを適切に対応してくれます
    - 後述のjs版インターフェースも内包している為、デバッグコンソールからのテスト実行も可能です


# cljs版の使い方

[js版の使い方はこちら](#js版の使い方)。

ここのセクションはcljs環境向けのものなので、js版のみ使いたい方は飛ばしてしまって問題ありません(js向けのほぼ同一の内容が「[js版の使い方](#js版の使い方)」にも書いてあります)。


## 前準備

1. `project.clj` の `:dependencies` に `[jp.ne.tir/vnctst-audio3 "X.Y.Z"]` を追加する
2. 利用したい名前空間にて `(:require [vnctst.audio3 :as audio3])` みたいな感じで require しておく
3. `(audio3/init!)` を実行しておく
    - ※これを実行し忘れていると「`Cannot read property 'x' of null`」みたいなエラーが出て動きません。忘れず実行してください。
    - 以下のキーワード引数を与える事により、挙動をカスタマイズする事が可能(しかし全省略でも問題ない)
        -  `:fallback-ext "拡張子"`
            - プリセット音源(後述)はoggを優先しますが、oggが再生できない場合の拡張子を指定します。省略時は `"mp3"` です。ツクールMV準拠にしたい場合は `"m4a"` にするとよいでしょう。
        - `:url-prefix "path/to/audio/"`
            - プリセット音源(後述)の配置urlのprefixを指定します。省略時は `"audio/"` です。通常用途では、末尾のスラッシュは必須です。通常はこれを変更する事はありません。
        -  `:dont-stop-on-background? 真偽値`
            - デフォルトでは、ページのタブがバックグラウンドになった場合に自動的にBGMを一時停止します(一部、対応していないブラウザもあります)。真値を指定する事でこの挙動を無効化できます。
        -  `:always-mute-at-mobile? 真偽値`
            - 真値を指定する事で、`User-Agent`ヘッダがモバイル環境っぽい場合に音響再生を常時ミュート状態にします。現時点ではモバイル対応に不完全な部分があり、もし不完全な対応を許容できない場合は真値を指定してください。
        -  `:debug? 真偽値`
            - 真値を指定する事で、再生動作の呼び出しや、ファイルのロードエラー等の各種情報をコンソールに出力するようになります。当ライブラリは基本的にエラーがあっても例外は投げないポリシーとしている為、エラーの検知にはこのオプションの設定が必須です。デバッグビルドでは真に、リリースビルドでは偽になるように上手く設定するとよいでしょう。
        -  `:never-use-webaudio? 真偽値`
            - WebAudioを常に無効化したい(つまりHtmlAudioのみ使うようにしたい)時に真値を指定します。これは基本的には当ライブラリの開発者向けの機能です。
    - この`init!`を複数回実行した場合、二回目以降の実行は無視されます(つまり後から上記パラメータを再設定する事はできません)


## 最も単純な使い方

- BGMを鳴らす(停止させるまでループ再生し続ける)
    - `(audio3/play-bgm! "path/to/hoge.ogg")`
        - urlも指定可能だが、その場合は[CORS設定](https://www.google.com/search?nfpr=1&q=CORS)が必要となるケースがある事に注意。
        - ブラウザからローカルhtmlファイルを直に開いた場合は上手く再生できない(これはどの再生ライブラリでも同じ)。何らかのhttpサーバ越しにアクセスする事。

- ME(非ループ曲)を再生する(曲の最後に到達したらそのまま再生を終了する)
    - `(audio3/play-me! "path/to/fuga.ogg")`

- 今鳴らしているBGM/MEをフェードアウトさせて(もしくは即座に)停止する
    - `(audio3/stop-bgm!)` フェードアウト1秒(デフォルト値)かけて停止
    - `(audio3/stop-bgm! 3)` フェードアウト3秒かけて停止
    - `(audio3/stop-bgm! 0)` 即座に停止
    - 既にBGMが停止状態の時は何も起こらない
    - 既にBGMがフェード中の時も基本的には何も起こらない。が、指定秒数が違う時のみ設定が上書きされる(適切に、フェードのまだ残っている部分が新しいフェード速度へと変更される)

- 今鳴っているBGMをフェードアウト終了させてから次の曲を再生する
    - `(audio3/play-bgm! "path/to/foo.ogg")`
        - これは上の「BGMを鳴らす」のと全く同じコードだが、これで「現在の曲をフェードアウト終了させてから次の曲を再生する」という挙動となる
        - 現在鳴らしている曲と全く同一の曲が指定された場合は何も起こらない
        - フェードアウト中に別の曲や同一曲の再生要求が行われた場合は、内部で適切に処理される事が保証される(同一曲だった場合はフェードインして元に戻る)。この辺りは内部では複雑な状態遷移を持つが、外から利用する際にそれを気にする必要はない。雑に扱ってよい。

- SE(効果音)を鳴らす(複数種類もしくは同一の音源ファイルを多重再生する事が可能)
    - `(audio3/play-se! "path/to/fuga.ogg")`
    - SEはイベント毎に鳴らすケースが多いが、複数のイベントが同時発火して同じ効果音を同時に鳴らしてしまうと、それが重なって結果として音量の増幅が起こり、音割れを起こす事がある。これを防ぐ為に、非常に近いタイムスライス(デフォルトでは50msec以内)での同じ効果音の再生は抑制するようにしている。


## プリセット指定の使い方

上記の「最も単純な使い方」では、直にpathを指定している。

しかし実際の再生環境では「oggが再生可能でない環境では、代わりにmp3やm4a等の別の音源ファイルを再生する」必要がある(何故かというと、oggもmp3もm4aも、「これを使っていれば、どのブラウザ環境でも再生可能だ」という訳にはいかない為)。

そこで、以下のような指定方法を可能とした。以後ここでは、この指定方法を「プリセット指定」と呼ぶ。

- `(audio3/play! :bgm/hoge)`
    - これは `(audio3/play-bgm! "audio/bgm/hoge.ogg")` もしくは `(audio3/play-bgm! "audio/bgm/hoge.mp3")` として実行される
        - どちらになるかは、oggが再生可能なら前者、そうでなければ後者となる
        - ここで実際に展開されるpath位置やogg再生不可時の拡張子等は、前述の `init!` のオプション値によって変更可能
        - もちろん、 `"audio/bgm/hoge.ogg"` と `"audio/bgm/hoge.mp3"` の両方のファイルを事前に設置しておく必要がある(片方だけでは駄目。ただし後述のcordova等で再生ブラウザ環境が固定できる場合、oggのみにする事は可能)。
    - プリセットのキーの`namespace`部(キーワードのスラッシュ以前の部分)に指定できるのは `bgm` `se` `bgs` `me` の四種類だけ。つまり `"audio/bgm/"` `"audio/se/"` `"audio/bgs/"` `"audio/me/"` の中に設置した音源ファイルのみがプリセット指定する事が可能となる(ここ以外に設置したファイルは文字列でのpath指定にするしかない)

- `(audio3/play! :se/fuga)`
    - 上記同様に `(audio3/play-se! "audio/se/fuga.ogg")` もしくは `(audio3/play-se! "audio/se/fuga.mp3")` として実行される

- `(audio3/play-me! :se/fuga)`
    - `(audio3/play-me! "audio/se/fuga.ogg")` もしくは `(audio3/play-me! "audio/se/fuga.mp3")` として実行される
    - `"audio/se/"` 配下に置いたファイルであっても、SE以外の種別(BGM/BGS/ME)として再生させる事は可能、というサンプル例(ただし、非常に分かりづらい)。

とりあえず、ここまでで「適当に`audio/`配下に配置した音源ファイルをBGMやSEとして一発で再生でき、しかもBGMフェードアウト処理や同一SE多重再生管理を適切に行ってくれる」ものとして利用できる。


## 基本音量を設定する

- 音量設定を変更/取得する
    - 音量設定値は全て0.0～1.0の数値で表現される。
    - `(audio3/set-volume-master! 0.5)`
        - マスターボリュームを設定する。初期値は0.5。
        - マスターボリュームは全体に影響する音量。初期状態の場合、マスターは0.5、個別ボリュームも0.5なので、結果として、何も設定しない場合は最大音量の25%で再生される事になる。
    - `(audio3/set-volume-bgm! 0.5)`
        - BGM/BGS/MEの基本ボリュームを設定する(SE以外は共通)。初期値は0.5。
    - `(audio3/set-volume-se! 0.5)`
        - SEの基本ボリュームを設定する。初期値は0.5。
    - `(audio3/get-volume-master)` `=>` 0.0～1.0
        - マスターボリュームを取得する
    - `(audio3/get-volume-bgm)` `=>` 0.0～1.0
        - BGM/BGS/MEの基本ボリュームを取得する
    - `(audio3/get-volume-se)` `=>` 0.0～1.0
        - SEの基本ボリュームを取得する


## 少し複雑な使い方

- この環境にて、特定種別の音源ファイルが再生可能かを調べる
    - `(audio3/can-play-ogg?)` `=>` `true or false`
    - `(audio3/can-play-mp3?)` `=>` `true or false`
    - `(audio3/can-play-m4a?)` `=>` `true or false`
    - `(audio3/can-play? "audio/ogg")` `=>` `true or false`
    - このチェックを行わずに直に再生しようとして失敗しても別にエラーは投げられない(デバッグフラグをオンにしていればコンソールにエラー情報は出力される)ので、「どうしても再生されないと困る」ような状況でないなら、このチェックを行わなくても別に問題はない。また前述のプリセット指定を行えば、内部で適切にこの判定を行ってくれる

- 再生開始時に、個別にvolume(音量), pitch(再生速度レート), pan(左右のバランス)を指定する
    - `(audio3/play! :bgm/hoge 1.5 1.2 -1.0)`
    - `(audio3/play! :se/fuga 0.15 0.8 1.0)`
    - `(audio3/play-bgm! "path/to/hoge.ogg" 1.5 1.2 -1.0)`
    - `(audio3/play-se! "path/to/fuga.ogg" 0.15 0.8 1.0)`
    - `play!` `play-bgm!` `play-se!` 等に対して、更に追加の引数として `volume` `pitch` `pan` の三つの数値を指定可能
        - volumeは0以上の小数値を指定する。省略時は 1.0 が指定されたものとして扱われる。
            - ここには1.0以上の値を指定する事が可能。ただし前述のマスターボリュームおよび各ボリューム設定を乗算した最終結果が1.0以上になる場合は1.0になる状態で頭打ちとなる(つまり初期状態のままなら、volumeに4.0以上を指定しても、4.0を指定した時と同じ再生音量となり、それよりも大きくする事はできない)
        - pitchは 0.1 ～ 10.0 の小数値を指定する。省略時は 1.0 として扱われる。小さくするとゆっくりした再生となり、大きくすると加速した再生となる
        - panは -1.0 ～ 1.0 の小数値を指定する。省略時は 0 として扱われる。 -1.0 が最も左寄り、 1.0 が最も右寄りとなる
        - pitchおよびpanについては、反映されない実行環境がある事に注意(WebAudioであっても非対応なブラウザが普通にある)

- 効果音を途中で停止させる
    - `(go (let [se-ch (audio3/play-se! "path/to/fuga.ogg")] (<! (cljs.core.async/timeout 1000)) (audio3/stop-se! se-ch)))`
    - `audio3/play-se!` (および `audio3/play!` にて `:se/*` 系を指定した場合)は再生ごとのチャンネルを返す。 `audio3/stop-se!` には、この再生チャンネルを引数として渡す必要がある
    - 既に再生完了したチャンネルを `audio3/stop-se!` に渡しても、何も起こらない
    - 途中で停止させる必要がなければ、`play-se!`の返すチャンネルは無視しても問題ない(再生終了後に適切にGCされる)

- BGMが再生中かどうかを調べる
    - `(audio3/playing-bgm?)`
        - BGM, MEが再生中なら真値を返す


## BGSを使う

- BGSを再生/停止する
    - BGSとは、BGMと並行して再生できるループ音源。人ごみの音、風の音、雨音、といった環境音を、BGMと同時に再生する事を想定している
    - `(audio3/play-bgs! "path/to/foo.ogg")`
    - `(audio3/play! :bgs/foo)`
    - `(audio3/stop-bgs!)`
    - 追加の引数についてはBGMと同じ

- BGSが再生中かどうかを調べる
    - `(audio3/playing-bgs?)`
        - BGSが再生中なら真値を返す


## プリロードを使う

- SEのプリロードを行う
    - `(audio3/preload-se! "path/to/fuga.ogg")`
    - `(audio3/preload-se! :se/fuga)`
    - 事前にプリロードを行う事で、初回再生時に内部で音源ファイルのローディングを行わずにすみ、初回再生時でも遅延なく即座にSEを再生できるようになる。
    - プリロードは非同期実行される。

- SEのプリロードが完了しているかを調査する
    - `(audio3/loaded-se? "path/to/fuga.ogg")`
    - `(audio3/loaded-se? :se/fuga)`
        - プリロードが完了しているなら真値を返す。
        - `play-se!` (および `play!` に `:se/*` 系のキーを指定した場合)も内部でプリロードを行っている為、 `preload-se!` を実行していなくても、一度でも鳴らした事のあるSEは、これによって真値が返る。
        - 注意点として、ロード時に何らかのエラー(ファイルがない等)が発生したケースでも真値を返す(「エラーが起こったが、ロード自体は完了した」という扱い)。エラーを検出したい場合は後述の `succeeded-to-load-se?` を使う事。
    - `(audio3/succeeded-to-load-se? "path/to/fuga.ogg")`
    - `(audio3/succeeded-to-load-se? :se/fuga)`
        - プリロードが完了し、再生が可能な状態になっていれば真値を返す。
        - `loaded-se?` との違いはロードエラーの扱い。エラー時は偽値になる。
        - これ単体ではロード完了待ちには使えない事に注意。ロード完了待ちをするには前述の `loaded-se?` の方が適切。
    - プリロード中に `play-se!` が実行された場合、プリロードが完了するまで再生は遅延する(通常のプリロードなし初回再生の時と同じ挙動)。

- SEのアンロードを行う
    - `(audio3/unload-se! "path/to/fuga.ogg")`
    - `(audio3/unload-se! :se/fuga)`
        - 全てのSEはプリロードもしくは再生の際に内部的にキャッシュされる。よって、「一度鳴らしたらもう二度と鳴らさない」ようなSEをどんどん大量に鳴らすようなケースでは、アンロードを行わないとメモリリークしてしまう。
        - アンロード後は、前述の `loaded-se?` と `succeeded-to-load-se?` は偽値を返すようになる。
        - アンロード後に、アンロードしたSEを再生しようとした場合、またプリロードして内部キャッシュしてからの再生となる。
        - プリロード中にアンロードを行った場合、プリロード自体が取り消される。

- BGM系のプリロードを行う
    - `(audio3/preload-bgm! "path/to/hoge.ogg")`
    - `(audio3/preload-bgm! :bgm/hoge)`
    - BGM/BGS/MEはどれも「BGM系」として、プリロード時には全て同じ扱いとなる
    - 注意点として、BGM系はSEとは違い、単に `play-bgm!` 等を実行しただけでは内部キャッシュ状態とはならない。なぜならBGM系は「即座に再生しないといけない」という要件があるケースがあまりなく、また一曲のメモリ消費が大きい場合が多いからである。明示的に `preload-bgm!` を実行した時のみ、プリロード状態になる。

- BGM系のプリロードが完了しているかを調査する
    - `(audio3/preloaded-bgm? "path/to/hoge.ogg")`
    - `(audio3/preloaded-bgm? :bgm/hoge)`
    - `(audio3/succeeded-to-preload-bgm? "path/to/hoge.ogg")`
    - `(audio3/succeeded-to-preload-bgm? :bgm/hoge)`
    - これらについてはSEのものとほぼ同様。
    - 明示的にプリロード完了を待たなくても `play-bgm!` 系の実行は行える(もちろんロード完了待ちは発生するが)。

- BGM系のアンロードを行う
    - `(audio3/unload-bgm! "path/to/hoge.ogg")`
    - `(audio3/unload-bgm! :bgm/hoge)`
    - これもSEのものとほぼ同様。


## プリセットのプリロード

プリセット音源のみ、まとめて(個別にファイルを指定せずに)プリロードを行う事ができる。

(プリロードを行わなくてもプリセットは利用可能だが、その場合は初回実行時にロードによる再生遅延が発生する)

- cljsプロジェクトの `resources/public/audio/` 配下の `bgm/` `bgs/` `me/` `se/` ディレクトリが、プリセット音源の各ディレクトリとして扱われる。ここに各音源ファイルを入れておく

以下の実行により、BGM/BGS/ME/SE別に、全プリセットのプリロードが行える

- `(audio3/preload-all-bgm-preset!)`
- `(audio3/preload-all-bgs-preset!)`
- `(audio3/preload-all-me-preset!)`
- `(audio3/preload-all-se-preset!)`

以下の実行により、全プリセットのプリロードが行える

- `(audio3/preload-all-preset!)`
    - ただし、BGMはプリロードしない方がいい場合が多い。何故なら、BGMが大量にある場合に全BGMをオンメモリを保持するのはメモリコストが高い為。なので通常は `(audio3/preload-all-se-preset!)` 等を個別に実行する事を検討した方がよい。

もしプリセット側に何らかの不備(oggとmp3の両方が揃っていない等)がある場合、上記のプリロード実行時にalertが表示される。

- node-webkitやcordova等の環境で、「ブラウザ環境が固定されているからoggだけの提供でよい」という場合は、これらの関数の引数に `true` を渡す事で、このalertを抑制できる。

上記のプリロード対象ファイルの一覧の取得は、コンパイルフェーズに行われる。

この一覧はプリセットキーワード化されて、以下の変数に保持されている。

- `audio3/preset-bgm-keys`
- `audio3/preset-bgs-keys`
- `audio3/preset-me-keys`
- `audio3/preset-se-keys`

このプリロード処理は非同期(バックグラウンド)で行われる為、「最初にローディング画面を出して、プリロードが完了するのを待つ」ような挙動を行いたい場合は、上記のプリセットキーワード一覧を保持している変数から、定期的に `preloaded-bgm?` および `loaded-se?` を実行し、全てのキーが真値を返すのを待つようにすればよい。真値の個数を数える事によってプログレス表示を行う事も可能。


## その他の機能

- 内部で利用している、再生環境種別を取得する
    - `audio3/terminal-type`
        - `#{:tablet :mobile :android :ios :chrome :firefox}` のようなキーワードの入ったset
            - `:tablet` はタブレットっぽい端末全般(iPad, android tablet, windows tablet)
            - `:mobile` は携帯っぽい端末全般(iPhone, android, windows phone)
            - `:android` はandroid全般
            - `:ios` はiOS全般
            - `:chrome` はchrome全般(PC/モバイル向けの両方含む)
            - `:firefox` はfirefox全般(PC/モバイル向けの両方含む)
        - 上記の判定は `User-Agent` ヘッダを元に判定している為、実際の再生環境とは異なる場合が普通にある。

- バックグラウンド時にも強制的にSEを鳴らす
    - `(audio3/alarm! "path/to/fuga.ogg")`
    - `(audio3/alarm! :se/fuga)`
    - 名前の通り、アラーム通知用途
    - 追加引数等は `play-se!` と同様

- ボリューム値変換ユーティリティ
    - `(audio3/float->percent f)` `=>` 0～100
        - vnctst-audio3でのボリューム値(0.0～1.0)を、パーセント表示する為の0～100の整数値に変換するユーティリティ。
    - `(audio3/percent->float p)` `=>` 0.0～1.0
        - `float->percent` の逆変換を行う。


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
```

~~~
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
~~~

```
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

~~~
;; path指定の場合は、再生可能メディア種別の判定を行った方がより良い
(if (audio3/can-play-ogg?)
  (audio3/play-se! "path/to/hoge.ogg")
  (audio3/play-se! "path/to/hoge.mp3"))

(audio3/can-play-mp3?)
(audio3/can-play-m4a?)
(audio3/can-play? "audio/ogg")

;; volume, pitch, pan を指定して再生
(audio3/play! :bgm/bach 1.5 1.2 -1)

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
~~~

```
;; 個別プリロード/アンロード
(go
  (audio3/preload-bgm! "path/to/hoge.ogg")
  (audio3/preload-se! :se/beep)
  (<! (async/timeout 10000))
  ;; 先にプリロードしておけば、初回再生時でもロード待ち無しに再生可能
  (audio3/play-bgm! "path/to/hoge.ogg")
  (audio3/play! :se/beep)
  (<! (async/timeout 10000))
  ;; 今後もう再生しないなら、アンロードしてメモリを解放しておく
  (audio3/unload-bgm! "path/to/hoge.ogg")
  (audio3/unload-se! :se/beep)
  )

;; ME再生が終わるまで待つサンプル
(do
  (audio3/play! :me/jingle)
  (go-loop []
    (<! (async/timeout 1000))
    (if (audio3/playing-bgm?)
      (recur)
      (js/console.log "Done to play BGM"))))

;; その他のユーティリティ
(when-not (:mobile audio3/terminal-type)
  (audio3/play! :se/beep))
```


# js版の使い方

[cljs版の使い方はこちら](#cljs版の使い方)

これは本来cljs向けのものなので、js版では以下の制約がある。

- ファイルサイズが大きく、ロードに時間がかかる
- 他のcljsベースのjsライブラリと同時に利用できない(ほとんどないとは思うが)
- コンパイルフェーズがない為、プリセットのまとめプリロードができない(個別プリロードは可能)

ファイルサイズ問題については、vnctst-audio3の仕様を[Specifications](#specifications)セクションにて公開しているので、やる気のある方はjsやtsに移植してもよいでしょう。


## 前準備

1. 同梱の `vnctst-audio3.js` ファイルを適切な場所に配置する
    - `vnctst-audio3.js` は https://github.com/ayamada/vnctst-audio3/releases からもダウンロードできます
2. htmlファイルに `<script src="vnctst-audio3.js" type="text/javascript"></script>` タグを入れ、`vnctst-audio3.js`ファイルをロードする
    - 前述の通りファイルサイズが大きい為、このscriptタグを入れるのはbodyの最後にして、ロードが完了するまではLOADING表示を出す等の対応をした方がより良い
3. 何らかのhttpサーバを用意し、そこ経由でhtmlファイルを開く
    - ローカルhtmlファイルをそのままブラウザで開くと、音響ファイルのロードに失敗する為(これはどの再生ライブラリでも同じ)
    - httpサーバを用意するのが面倒な場合は、[Dropbox](https://www.dropbox.com/ja/)のpublicフォルダ内のどこかにhtmlファイルを配置し、その公開リンクの方をブラウザで開けば手軽に確認できる。おすすめ。
4. `vnctst.audio3.js.init()` を実行する
    - ※これを実行し忘れていると「`Cannot read property 'x' of null`」みたいなエラーが出て動きません。忘れず実行してください。
    - `vnctst.audio3.js.init({...})` のように引数を与える事で挙動をカスタマイズ可能(省略可)。パラメータは以下の通り
        -  `"fallback-ext": "拡張子"`
            - プリセット音源(後述)はoggを優先しますが、oggが再生できない場合の拡張子を指定します。省略時は `"mp3"` です。ツクールMV準拠にしたい場合は `"m4a"` にするとよいでしょう。
        - `"url-prefix": "path/to/audio/"`
            - プリセット音源(後述)の配置urlのprefixを指定します。省略時は `"audio/"` です。通常用途では、末尾のスラッシュは必須です。通常はこれを変更する事はありません。
        -  `"dont-stop-on-background?": 真偽値`
            - デフォルトでは、ページのタブがバックグラウンドになった場合に自動的にBGMを一時停止します(一部、対応していないブラウザもあります)。真値を指定する事でこの挙動を無効化できます。
        -  `"always-mute-at-mobile?": 真偽値`
            - 真値を指定する事で、`User-Agent`ヘッダがモバイル環境っぽい場合に音響再生を常時ミュート状態にします。現時点ではモバイル対応に不完全な部分があり、もし不完全な対応を許容できない場合は真値を指定してください。
        -  `"debug?": 真偽値`
            - 真値を指定する事で、再生動作の呼び出しや、ファイルのロードエラー等の各種情報をコンソールに出力するようになります。当ライブラリは基本的にエラーがあっても例外は投げないポリシーとしている為、エラーの検知にはこのオプションの設定が必須です。デバッグビルドでは真に、リリースビルドでは偽になるように上手く設定するとよいでしょう。
        -  `"never-use-webaudio?": 真偽値`
            - WebAudioを常に無効化したい(つまりHtmlAudioのみ使うようにしたい)時に真値を指定します。これは基本的には当ライブラリの開発者向けの機能です。

なお `vnctst.audio3.js.init()` が長くて嫌な場合は `var va3 = vnctst.audio3.js` を実行しておけば `va3.init()` で実行できます。これは`init()`以外でも同様です。


## 最も単純な使い方

- BGMを鳴らす(停止させるまでループ再生し続ける)
    - `vnctst.audio3.js.playBgm("path/to/hoge.ogg")`
        - urlも指定可能だが、その場合は[CORS設定](https://www.google.com/search?nfpr=1&q=CORS)が必要となるケースがある事に注意。
        - ブラウザからローカルhtmlファイルを直に開いた場合は上手く再生できない(これはどの再生ライブラリでも同じ)。何らかのhttpサーバ越しにアクセスする事。

- ME(非ループ曲)を再生する(曲の最後に到達したらそのまま再生を終了する)
    - `vnctst.audio3.js.playMe("path/to/fuga.ogg")`

- 今鳴らしているBGM/MEをフェードアウトさせて(もしくは即座に)停止する
    - `vnctst.audio3.js.stopBgm()` フェードアウト1秒(デフォルト値)かけて停止
    - `vnctst.audio3.js.stopBgm(3)` フェードアウト3秒かけて停止
    - `vnctst.audio3.js.stopBgm(0)` 即座に停止
    - 既にBGMが停止状態の時は何も起こらない
    - 既にBGMがフェード中の時も基本的には何も起こらない。が、指定秒数が違う時のみ設定が上書きされる(適切に、フェードのまだ残っている部分が新しいフェード速度へと変更される)

- 今鳴っているBGMをフェードアウト終了させてから次の曲を再生する
    - `vnctst.audio3.js.playBgm("path/to/foo.ogg")`
        - これは上の「BGMを鳴らす」のと全く同じコードだが、これで「現在の曲をフェードアウト終了させてから次の曲を再生する」という挙動となる
        - 現在鳴らしている曲と全く同一の曲が指定された場合は何も起こらない
        - フェードアウト中に別の曲や同一曲の再生要求が行われた場合は、内部で適切に処理される事が保証される(同一曲だった場合はフェードインして元に戻る)。この辺りは内部では複雑な状態遷移を持つが、外から利用する際にそれを気にする必要はない。雑に扱ってよい。

- SE(効果音)を鳴らす(複数種類もしくは同一の音源ファイルを多重再生する事が可能)
    - `vnctst.audio3.js.playSe("path/to/fuga.ogg")`
    - SEはイベント毎に鳴らすケースが多いが、複数のイベントが同時発火して同じ効果音を同時に鳴らしてしまうと、それが重なって結果として音量の増幅が起こり、音割れを起こす事がある。これを防ぐ為に、非常に近いタイムスライス(デフォルトでは50msec以内)での同じ効果音の再生は抑制するようにしている。


## プリセット指定の使い方

上記の「最も単純な使い方」では、直にpathを指定している。

しかし実際の再生環境では「oggが再生可能でない環境では、代わりにmp3やm4a等の別の音源ファイルを再生する」必要がある(何故かというと、oggもmp3もm4aも、「これを使っていれば、どのブラウザ環境でも再生可能だ」という訳にはいかない為)。

そこで、以下のような指定方法を可能とした。以後ここでは、この指定方法を「プリセット指定」と呼ぶ。

- `vnctst.audio3.js.play({bgm:"hoge"})`
    - これは `vnctst.audio3.js.playBgm("audio/bgm/hoge.ogg")` もしくは `vnctst.audio3.js.playBgm("audio/bgm/hoge.mp3")` として実行される
        - どちらになるかは、oggが再生可能なら前者、そうでなければ後者となる
        - ここで実際に展開されるpath位置やogg再生不可時の拡張子等は、前述の `init()` のオプション値によって変更可能
        - もちろん、 `"audio/bgm/hoge.ogg"` と `"audio/bgm/hoge.mp3"` の両方のファイルを事前に設置しておく必要がある(片方だけでは駄目。ただし後述のcordova等で再生ブラウザ環境が固定できる場合、oggのみにする事は可能)。
    - プリセットのkey部に指定できるのは `bgm` `se` `bgs` `me` の四種類だけ。つまり `"audio/bgm/"` `"audio/se/"` `"audio/bgs/"` `"audio/me/"` の中に設置した音源ファイルのみがプリセット指定する事が可能となる(ここ以外に設置したファイルは文字列でのpath指定にするしかない)

- `vnctst.audio3.js.play({se:"fuga"})`
    - 上記同様に `vnctst.audio3.js.playSe("audio/se/fuga.ogg")` もしくは `vnctst.audio3.js.playSe({"audio/se/fuga.mp3")` として実行される

- `vnctst.audio3.js.playMe({se:"fuga"})`
    - `vnctst.audio3.js.playMe("audio/se/fuga.ogg")` もしくは `vnctst.audio3.js.playMe("audio/se/fuga.mp3")` として実行される
    - `"audio/se/"` 配下に置いたファイルであっても、SE以外の種別(BGM/BGS/ME)として再生させる事は可能、というサンプル例(ただし、非常に分かりづらい)。

とりあえず、ここまでで「適当に`audio/`配下に配置した音源ファイルをBGMやSEとして一発で再生でき、しかもBGMフェードアウト処理や同一SE多重再生管理を適切に行ってくれる」ものとして利用できる。


## 基本音量を設定する

- 音量設定を変更/取得する
    - 音量設定値は全て0.0～1.0の数値で表現される。
    - `vnctst.audio3.js.setVolumeMaster(0.5)`
        - マスターボリュームを設定する。初期値は0.5。
        - マスターボリュームは全体に影響する音量。初期状態の場合、マスターは0.5、個別ボリュームも0.5なので、結果として、何も設定しない場合は最大音量の25%で再生される事になる。
    - `vnctst.audio3.js.setVolumeBgm(0.5)`
        - BGM/BGS/MEの基本ボリュームを設定する(SE以外は共通)。初期値は0.5。
    - `vnctst.audio3.js.setVolumeSe(0.5)`
        - SEの基本ボリュームを設定する。初期値は0.5。
    - `vnctst.audio3.js.getVolumeMaster()` `=>` 0.0～1.0
        - マスターボリュームを取得する
    - `vnctst.audio3.js.getVolumeBgm()` `=>` 0.0～1.0
        - BGM/BGS/MEの基本ボリュームを取得する
    - `vnctst.audio3.js.getVolumeSe()` `=>` 0.0～1.0
        - SEの基本ボリュームを取得する


## 少し複雑な使い方

- この環境にて、特定種別の音源ファイルが再生可能かを調べる
    - `vnctst.audio3.js.canPlayOgg()` `=>` `true or false`
    - `vnctst.audio3.js.canPlayMp3()` `=>` `true or false`
    - `vnctst.audio3.js.canPlayM4a()` `=>` `true or false`
    - `vnctst.audio3.js.canPlay("audio/ogg")` `=>` `true or false`
    - このチェックを行わずに直に再生しようとして失敗しても別にエラーは投げられない(デバッグフラグをオンにしていればコンソールにエラー情報は出力される)ので、「どうしても再生されないと困る」ような状況でないなら、このチェックを行わなくても別に問題はない。また前述のプリセット指定を行えば、内部で適切にこの判定を行ってくれる

- 再生開始時に、個別にvolume(音量), pitch(再生速度レート), pan(左右のバランス)を指定する
    - `vnctst.audio3.js.play({bgm:"hoge"}, 1.5, 1.2, -1.0)`
    - `vnctst.audio3.js.play({se:"fuga"}, 0.15, 0.8, 1.0)`
    - `vnctst.audio3.js.playBgm("path/to/hoge.ogg", 1.5, 1.2, -1.0)`
    - `vnctst.audio3.js.playSe("path/to/fuga.ogg", 0.15, 0.8, 1.0)`
    - `play` `playBgm` `playSe` 等に対して、更に追加の引数として `volume` `pitch` `pan` の三つの数値を指定可能
        - volumeは0以上の小数値を指定する。省略時は 1.0 が指定されたものとして扱われる。
            - ここには1.0以上の値を指定する事が可能。ただし前述のマスターボリュームおよび各ボリューム設定を乗算した最終結果が1.0以上になる場合は1.0になる状態で頭打ちとなる(つまり初期状態のままなら、volumeに4.0以上を指定しても、4.0を指定した時と同じ再生音量となり、それよりも大きくする事はできない)
        - pitchは 0.1 ～ 10.0 の小数値を指定する。省略時は 1.0 として扱われる。小さくするとゆっくりした再生となり、大きくすると加速した再生となる
        - panは -1.0 ～ 1.0 の小数値を指定する。省略時は 0 として扱われる。 -1.0 が最も左寄り、 1.0 が最も右寄りとなる
        - pitchおよびpanについては、反映されない実行環境がある事に注意(WebAudioであっても非対応なブラウザが普通にある)

- 効果音を途中で停止させる
    - `var seCh = vnctst.audio3.js.playSe("path/to/fuga.ogg");` `setTimeout(function () { vnctst.audio3.js.stopSe(seCh) }, 1000);`
    - `vnctst.audio3.js.playSe()` (および `vnctst.audio3.js.play()` にて `{se:"..."}` 系を指定した場合)は再生ごとのチャンネルを返す。 `vnctst.audio3.js.stopSe()` には、この再生チャンネルを引数として渡す必要がある
    - 既に再生完了したチャンネルを `vnctst.audio3.js.stopSe()` に渡しても、何も起こらない
    - 途中で停止させる必要がなければ、`playSe()`の返すチャンネルは無視しても問題ない(再生終了後に適切にGCされる)

- BGMが再生中かどうかを調べる
    - `vnctst.audio3.js.isPlayingBgm()`
        - BGM, MEが再生中なら真値を返す


## BGSを使う

- BGSを再生/停止する
    - BGSとは、BGMと並行して再生できるループ音源。人ごみの音、風の音、雨音、といった環境音を、BGMと同時に再生する事を想定している
    - `vnctst.audio3.js.playBgs("path/to/foo.ogg")`
    - `vnctst.audio3.js.play({bgs:"foo"})`
    - `vnctst.audio3.js.stopBgs()`
    - 追加の引数についてはBGMと同じ

- BGSが再生中かどうかを調べる
    - `vnctst.audio3.js.isPlayingBgs()`
        - BGSが再生中なら真値を返す


## プリロードを使う

- SEのプリロードを行う
    - `vnctst.audio3.js.preloadSe("path/to/fuga.ogg")`
    - `vnctst.audio3.js.preloadSe({se:"fuga"})`
    - 事前にプリロードを行う事で、初回再生時に内部で音源ファイルのローディングを行わずにすみ、初回再生時でも遅延なく即座にSEを再生できるようになる。
    - プリロードは非同期実行される。

- SEのプリロードが完了しているかを調査する
    - `vnctst.audio3.js.isLoadedSe("path/to/fuga.ogg")`
    - `vnctst.audio3.js.isLoadedSe({se:"fuga"})`
        - プリロードが完了しているなら真値を返す。
        - `playSe()` (および `play()` に `{se:"..."}` 系のキーを指定した場合)も内部でプリロードを行っている為、 `preloadSe()` を実行していなくても、一度でも鳴らした事のあるSEは、これによって真値が返る。
        - 注意点として、ロード時に何らかのエラー(ファイルがない等)が発生したケースでも真値を返す(「エラーが起こったが、ロード自体は完了した」という扱い)。エラーを検出したい場合は後述の `isSucceededToLoadSe()` を使う事。
    - `vnctst.audio3.js.isSucceededToLoadSe("path/to/fuga.ogg")`
    - `vnctst.audio3.js.isSucceededToLoadSe({se:"fuga"})`
        - プリロードが完了し、再生が可能な状態になっていれば真値を返す。
        - `isLoadedSe()` との違いはロードエラーの扱い。エラー時は偽値になる。
        - これ単体ではロード完了待ちには使えない事に注意。ロード完了待ちをするには前述の `isLoadedSe()` の方が適切。
    - プリロード中に `playSe()` が実行された場合、プリロードが完了するまで再生は遅延する(通常のプリロードなし初回再生の時と同じ挙動)。

- SEのアンロードを行う
    - `vnctst.audio3.js.unloadSe("path/to/fuga.ogg")`
    - `vnctst.audio3.js.unloadSe({se:"fuga"})`
        - 全てのSEはプリロードもしくは再生の際に内部的にキャッシュされる。よって、「一度鳴らしたらもう二度と鳴らさない」ようなSEをどんどん大量に鳴らすようなケースでは、アンロードを行わないとメモリリークしてしまう。
        - アンロード後は、前述の `isLoadedSe()` と `isSucceededToLoadSe()` は偽値を返すようになる。
        - アンロード後に、アンロードしたSEを再生しようとした場合、またプリロードして内部キャッシュしてからの再生となる。
        - プリロード中にアンロードを行った場合、プリロード自体が取り消される。

- BGM系のプリロードを行う
    - `vnctst.audio3.js.preloadBgm("path/to/hoge.ogg")`
    - `vnctst.audio3.js.preloadBgm({bgm:"hoge"})`
    - BGM/BGS/MEはどれも「BGM系」として、プリロード時には全て同じ扱いとなる
    - 注意点として、BGM系はSEとは違い、単に `playBgm()` 等を実行しただけでは内部キャッシュ状態とはならない。なぜならBGM系は「即座に再生しないといけない」という要件があるケースがあまりなく、また一曲のメモリ消費が大きい場合が多いからである。明示的に `preloadBgm()` を実行した時のみ、プリロード状態になる。

- BGM系のプリロードが完了しているかを調査する
    - `vnctst.audio3.js.isPreloadedBgm("path/to/hoge.ogg")`
    - `vnctst.audio3.js.isPreloadedBgm({bgm:"hoge"})`
    - `vnctst.audio3.js.isSucceededToPreloadBgm("path/to/hoge.ogg")`
    - `vnctst.audio3.js.isSucceededToPreloadBgm({bgm:"hoge"})`
    - これらについてはSEのものとほぼ同様。
    - 明示的にプリロード完了を待たなくても `playBgm()` 系の実行は行える(もちろんロード完了待ちは発生するが)。

- BGM系のアンロードを行う
    - `vnctst.audio3.js.unloadBgm("path/to/hoge.ogg")`
    - `vnctst.audio3.js.unloadBgm({bgm:"hoge"})`
    - これもSEのものとほぼ同様。


## プリセットのプリロード

js版では、プリセットのまとめプリロードには非対応。

上記の個別指定でのプリロードを行ってください。


## その他の機能

- バージョン文字列を取得する
    - `vnctst.audio3.js.version` `=>` `"0.1.0"`

- 内部で利用している、再生環境種別を取得する
    - 今のところjs版では未対応。時間のある時に対応します

- バックグラウンド時にも強制的にSEを鳴らす
    - `vnctst.audio3.js.alarm("path/to/fuga.ogg")`
    - `vnctst.audio3.js.alarm({se:"fuga"})`
    - 名前の通り、アラーム通知用途
    - 追加引数等は `playSe()` と同様

- ボリューム値変換ユーティリティ
    - `vnctst.audio3.js.floatToPercent(f)` `=>` 0～100
        - vnctst-audio3でのボリューム値(0.0～1.0)を、パーセント表示する為の0～100の整数値に変換するユーティリティ。
    - `vnctst.audio3.js.percentToFloat(p)` `=>` 0.0～1.0
        - `floatToPercent` の逆変換を行う。


## Cheat Sheet

~~~
var va3 = vnctst.audio3.js;

// 最初にこれを実行。引数は省略可能
va3.init({"fallback-ext": "mp3",
          "url-prefix": "audio/",
          "dont-stop-on-background?": false,
          "always-mute-at-mobile?": true,
          "debug?": true,
          "never-use-webaudio?": false});

// ベース音量の設定および取得
va3.setVolumeMaster(0.5);
va3.setVolumeBgm(0.5);
va3.setVolumeSe(0.5);

console.log(va3.getVolumeMaster());
console.log(va3.getVolumeBgm());
console.log(va3.getVolumeSe());
~~~

```
// 再生
va3.play({bgm:"bach"});
va3.play({bgs:"wind"});
va3.play({me:"jingle"});
va3.play({se:"beep"});

va3.playBgm("path/to/bach.ogg");
va3.playBgs("path/to/wind.ogg");
va3.playMe("path/to/jingle.ogg");
va3.playSe("path/to/beep.ogg");
```

~~~
// path指定の場合は、再生可能メディア種別の判定を行った方がより良い
if (va3.canPlayOgg()) {
  va3.playSe("path/to/hoge.ogg");
}
else {
  va3.playSe("path/to/hoge.mp3");
}

console.log(va3.canPlayMp3());
console.log(va3.canPlayM4a());
console.log(va3.canPlay("audio/ogg"));

// volume, pitch, pan を指定して再生
va3.play({bgm:"bach"}, 1.5, 1.2, -1);

// バックグラウンド消音を無視してSE再生
va3.alarm({se:"beep"});

// 停止
va3.stopBgm(); // フェードアウト1秒で停止
va3.stopBgm(3); // フェードアウト3秒で停止
va3.stopBgm(0); // フェードアウトなしで即座に停止
va3.stopBgs(); // BGSの停止は別扱い(BGMとMEは共通)

// SEの停止にはSEの再生チャンネルを指定する必要がある
var ch = va3.play({se:"siren"});
setTimeout(function () { va3.stopSe(ch) }, 500);
~~~

```
// 個別プリロード/アンロード
va3.preloadBgm("path/to/hoge.ogg");
va3.preloadSe({se:"beep"});
...
// 先にプリロードしておけば、初回再生時でもロード待ち無しに再生可能
va3.playBgm("path/to/hoge.ogg");
va3.play({se:"beep"});
...
// 今後もう再生しないなら、アンロードしてメモリを解放しておく
va3.unloadBgm("path/to/hoge.ogg");
va3.unloadSe({se:"beep"});

// ME再生が終わるまで待つサンプル
va3.play({me:"jingle"});
function supervise() {
  if (va3.isPlayingBgm()) {
    setTimeout(supervise, 1000);
  }
  else {
    console.log("Done to play BGM");
  }
}
supervise();

// その他のユーティリティ
console.log(va3.version);
```


# 制約 / 提供しない機能 / FAQ

- 現在のところ、モバイル対応に不完全な部分がある
    - 完全な対応を行おうとする場合、 https://outcloud.blogspot.jp/2015/11/htmlaudio.html を全て満たす必要がある…
    - 将来には対応させたいところだが、いつになるかは未定

- RPGツクールでは、MEの再生完了時には、元のbgmにフェードインして戻りますが、vnctst-audioでは、MEの再生完了時はそのまま無音になる仕様です。
    - どうしてMEの仕様をツクールと違う仕様にしたのかというと、ゲーム進行とMEが同期する前提ならツクールの仕様の方が自然なのですが、非同期になる場合(BGMやMEが非常に頻繁に変化する場合)はものすごく不自然に聞こえるケースがあるので、どちらの場合でもまあまあ自然に使える現仕様としました。

- 他のライブラリで提供されているようなサウンドスプライト/オーディオスプライト機能は提供しません
    - HtmlAudio上でバックグラウンド状態にした際にタイマー精度が大幅に悪くなり切り出し判定が非常に大雑把になってしまう問題がある事、スプライト設定が煩雑になりがちな事、音源ファイルの事前結合が非常に面倒な事、個別プリロードを行う前提の場合は一つのファイルに結合するメリットが薄い事、これらを鑑みた結果として『ない方がシンプルで使いやすい』という結論になりました

- ogg, m4aの内部タグによるループ位置指定には対応しない
    - 非対応環境での対応が微妙すぎる事になる為

- 「再生完了時に指定したハンドルを実行する」機能は、標準では提供しません
    - 何故提供していないのかというと、環境によっては「再生自体行われない」ケースが普通にあり、その場合に「どのタイミングで完了ハンドルを実行するのがベストか」は一意には決まらないからです(即座実行？曲の長さだけ待って実行？)。
    - どうしてもそのような処理が必要な場合は、定期的に `playing-bgm?` を実行するcore.asyncスレッドを起動して監視してください(上記チートシート内のソース参照)。
        - なお、この場合は「雑な処理への対応外」となる為、他のスレッドからループ再生要求がされた場合の処理や、曲の終了待ちのキャンセル等への対応も自身で考えないといけない事に注意してください。

- `vnctst-audio3` の「3」って何？
    - 「VNCTST gamesの内部音源ライブラリの三代目」というところから来ています。
        - 初代(`vnctst-audio`)はデバイス部に [boombox](https://github.com/CyberAgent/boombox.js) を利用していました
        - 二代目(`vnctst-audio2`)はSEのみのサポートで、初代と組み合わせて使っていました(boomboxでは同一音源の多重再生がサポートされていない問題への対処として)
    - 今回のリリースにあたって、この「3」を取って `vnctst-audio` とする事も検討したのですが、既存の自作ゲーム内にて名前空間が衝突するのが嫌なのと、今後に新しいコードベースで「4」を作る可能性がある為、敢えて「3」を残す事としました。


# 必須知識

html5上の音響システム固有の、知っておくべき内容のメモ

- 音が出ない環境の事を常に考慮する必要がある
    - 以下の二点に注意すればとりあえずok
        - ユーザにレスポンスを返すのに音だけの表現にはしない(必ずビジュアルでのエフェクトも付ける。具体的にはボタンを押した時の反応等)
        - 再生環境によっては再生が一瞬で完了する(扱いになる)ので、音源の再生完了をトリガーとして何かを行うような事は避けた方がよい(代わりに`setTimeout()`等で実際に待つ秒数を指定する等)

- 一応、「oggとmp3」もしくは「oggとm4a」で、基本的なブラウザ環境(PCおよびモバイル)での再生はカバーできる、という事になっている
    - 「ogg」「mp3」「m4a」のいずれか一つだけだと、再生できない環境が出てしまう。よって「oggとmp3」「oggとm4a」のどちらかのセットを選ぶ事になる

- 違うドメインにあるファイルを再生する場合は[CORS設定](https://www.google.com/search?nfpr=1&q=CORS)が必要

- ブラウザからローカルファイルを直に開いた場合はまともに機能しない(これはどの音源ライブラリでも同じ)。ローカルで確認する際でも何らかのhttpサーバが必要になる

- ブラウザ上のページがバックグラウンドになった際に、jsのタイマー精度が1秒ぐらいに悪くなる(具体的には、jsで`setTimeout(xxx, 10)`とか指定してもxxxが実行されるのは1000msec後とかになる場合がある)
    - これを防止する事は基本的にはできない
        - WebAudio付属のタイマーやWebWorkers等の別の機構を使って回避する事は一応可能(しかし、色々とめんどいし環境に依存する)
    - なので、これが起こっても大丈夫なように色々と考えた方がよい

TODO: もっとあった筈なので、思い出し次第、あとで追記する


# TODO

以下のTODOは時間のある時に対応を行う予定です。

- `vnctst.audio3/terminal-type` のjs版での対応

- BGMフェードのデフォルト値を変更する機能

- SEの短期間での連打禁止の閾値を変更する機能

- SEを停止させる際のフェードアウト秒数指定
    - 現状では、常に0が指定された挙動になっている
    - ゲームキャラの台詞を音声再生するような用途にはMEではなくSEを使う事になり、その際にフェードアウトしたいケースがありえるので、これは時間のある時にきちんと対応させたい

- 古いandroidのchromeでのタッチイベントアンロックが上手く動いてない
    - 試してみたところ、タッチ系ハンドル内では再生されるが、そうでない場所では再生されていない。iOS系とは違い「アンロック」という概念自体がないのか、それともインスタンス生成等の位置に影響があるのか？
    - これは可能ならきちんと対応させたい。あとで調べる。

- BGM系のプリロード関連の関数名にいまいち分かりづらい点がある
    - `preload-bgm!` `preloaded-bgm?` `succeeded-to-preload-bgm?` `unload-bgm!` は、BGM/BGS/ME共用というのが分かりづらい
    - その一方、 `playing-bgm?` と `playing-bgs?` は分かれている
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

上記以外の不具合を発見した場合は、githubのissueから報告する事ができます(日本語でokです)。
ただし、よほど重要な問題でなければ基本的に対応はしない予定です(@ayamadaに対応する時間があるのなら、その時間をゲーム作成に割り当てたい為)。
とは言え、たとえ対応を行わないとしても「こういう不具合がある」という情報自体は実際有用ですので、issueへの報告を歓迎します。


# Specifications

[SPEC.md](SPEC.md) にて、 `vnctst-audio3` の内部仕様書を公開しています。

同様のライブラリを作りたい方は参考にできるかもしれません。


# Development

`vnctst-audio3` 自体の開発手順については [DEVEL.md](DEVEL.md) を参照してください。

cljs開発の知識がある事が前提です。


# License

zlib風ライセンスとします。

- ライセンスの条項全文は [LICENSE](LICENSE) にあります(英語)。
- 利用時にcopyright文等の表示条項等はありません。
- zlibライセンスの詳細は https://ja.wikipedia.org/wiki/Zlib_License 等で確認してください。


# ChangeLog

- 0.1.1 (XXXX-XX-XX 次リリース予定)
    - ドキュメントの修正と追加
    - `all-bgm-keys` `all-bgs-keys` `all-me-keys` `all-se-keys` は、
      `vec`ではなく`set`として保持するようにした
      (これにより `(:se/hoge audio3/all-bgm-keys)` で存在チェックができる)
    - travis-ciでの自動ビルド試験を追加

- 0.1.0 (2016-06-29)
    - 一通り動作確認を行ったので、これを公式な初回リリースとする
    - オンラインデモの説明文に少し追記
    - 開発向けドキュメントに少し追加

- 0.1.0-rc2 (2016-06-29)
    - js用モジュールの関数名を`snake_case`から`camelCase`へと変更
    - ドキュメントの修正と追加
    - dependenciesのバージョン上げ
    - ME終了後にバックグラウンド操作して復帰すると、
      また最初からMEが再生されてしまう不具合を修正
    - `play-bgm!`系が返り値として不要な内部オブジェクトを返していたので、
      代わりにtrueを返すように変更
    - 冗長な一部のデバッグログ出力を抑制
    - ブラウザゲームで利用しやすいように、ライセンスをzlibに変更
    - オンラインデモを大幅改善
    - デモ曲を新規に作成、追加

- 0.1.0-rc1 (2016-05-21)
    - 京都Clojureの為に、プレビュー版を作成
    - ドキュメントがまだ作成途中のまま

- X.X.X (2016-04-XX 非公開)
    - コア部分は完成
    - ドキュメントなし


