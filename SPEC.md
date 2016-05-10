# vnctst-audio3 仕様書

TODO




# 一時メモ(READMEから移動させてきたそのままの状態)

- android版chromeでの、WebAudioの音源ロードに異様に時間がかかる問題
  (この環境のBGM/BGS/MEのみ、WebAudioではなくHtmlAudioでの再生とする。
  ただし、この環境でもSEはWebAudioとする)


## 用語について

- device, (再生)デバイス : 物理的なデバイスではなく、
  WebAudioやHtmlAudio等(のラッパー層)を指す語。
    - デバイス層は低レベルの機能だけを提供する。
      提供する機能の詳細は後述。

- audio-source, as, 音源ソース : urlからロードされた音源データ。
  この実体はdeviceによってまちまちだが、単一のmapとして表現される。

- audio-channel, ac, channel, 再生チャンネル : 再生可能なインスタンス。
  この実体もdeviceによってまちまちだが、mapの入ったatomとして表現される。
    - acのインスタンスは、asから生成される。複数個の生成も可能。
    - 別々のacを同時に再生する事で「BGMとSEを同時に鳴らす」ような事を実現する。
      勿論、同一のasから生成した複数のacを多重に再生する事も可能。
    - acは再生後にもう一度再生したり、再生中に巻き戻したり、
      再生パラメータを変更したりできる。


## デバイス種別

- vnctst.audio3.device.dumb - コンソール出力のみのダミーデバイス
    - WebAudioもHtmlAudioも利用可能でない場合にこれが採用される。もちろん音は出ない。
- vnctst.audio3.device.web-audio - WebAudioラッパ
- vnctst.audio3.device.html-audio-single - HtmlAudioラッパ(単チャンネル再生)
    - モバイル環境を想定して、同一SEの多重再生を行わないタイプ。同一SEの多重再生リクエストがあった際には、先に鳴っていたSEを停止させてから新しいSEを再生させる。違うSEであれば普通に多重再生できる。
- vnctst.audio3.device.html-audio-multi - HtmlAudioラッパ(多チャンネル再生)
    - PC環境を想定して、同一SEの多重再生を行うタイプ。


# 付録

## 筆者による、他の音源ライブラリを試した時の感想

- SoundJS, Howler, Buzz - http://doc.tir.ne.jp/misc/karasumaclj/js-libraries#%E9%9F%B3%E6%BA%90%E5%86%8D%E7%94%9F%E3%83%A9%E3%82%A4%E3%83%96%E3%83%A9%E3%83%AA に書いた(2014年頃の情報だが)

- boombox - https://github.com/CyberAgent/boombox.js - 扱いやすいが、標準では同じSEの連打に対応していない。しかしそれ以外は特に問題なく使える為、旧vnctst-audioの内部デバイス部として利用していた



