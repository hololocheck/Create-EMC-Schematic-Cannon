# Create: EMC Schematic Cannon

![icon](説明用素材/icon.png)

**Create**の概略図キャノンと**ProjectE**のEMCシステムを組み合わせたNeoForge Mod。
AE2 MEストレージとの連携にも対応しています。

## 対応バージョン

- Minecraft **1.21.1**
- NeoForge **21.1.168+**

## 依存Mod

- [Create](https://modrinth.com/mod/create) 6.0.9+
- [ProjectE](https://modrinth.com/mod/projecte) 1.1.0+
- [Applied Energistics 2](https://modrinth.com/mod/ae2)（任意 - AE2連携機能用）

## 機能

### EMC概略図砲
- FE動力で概略図を超高速設置
- ProjectEのEMCを消費してブロックを生成
- AE2 MEストレージ / チェストからのアイテム取り出し
- 速度調整（1〜256 blocks/tick）
- 複数の置換モード（固体ブロック保護、ブロックエンティティ保護等）
- 概略図再利用オプション

### フィラーモード
- 埋め立て / 完全消去 / 撤去 / 壁 / タワー / 箱 / 円壁
- 範囲指定ボードで範囲を指定
- 撤去モードでのEMC変換 / ストレージ搬入

### 空中設置杖
- FEを消費してフレームブロックを空中に設置
- Shift+スクロールで設置距離調整（1〜15ブロック）
- Shift+右クリックで一括撤去

### 範囲指定ボード
- 2点を指定して範囲を設定
- 編集モードで個別の点を調整
- フィラーモード・撤去モードで使用

## ビルド方法

```bash
gradlew.bat build
```

出力: `build/libs/emcschematicannon-x.x.x.jar`

## ライセンス

All Rights Reserved
