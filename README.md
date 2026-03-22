# Create: EMC Schematic Cannon

<p align="center">
  <img src="説明用素材/icon.png" width="120" alt="Create: EMC Schematic Cannon">
</p>

<p align="center">
  <strong>Create × ProjectE × AE2 — All-in-one Building Automation</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.1-62B47A.svg" alt="Minecraft">
  <img src="https://img.shields.io/badge/NeoForge-21.1.168+-orange.svg" alt="NeoForge">
  <img src="https://img.shields.io/badge/version-1.1.0-blue.svg" alt="Version">
  <img src="https://img.shields.io/badge/license-All%20Rights%20Reserved-red.svg" alt="License">
</p>

[日本語](#japanese) | [English](#english)

---

<a id="japanese"></a>
## 🇯🇵 日本語 (Japanese)

**Create** の概略図キャノンと **ProjectE** のEMCシステムを組み合わせたNeoForge Mod です。AE2 MEストレージとの連携にも対応し、大規模建築の完全自動化を実現します。

### 📦 依存Mod
| Mod | バージョン | 必須 |
|-----|-----------|------|
| [Create](https://modrinth.com/mod/create) | 6.0.9+ | ✅ |
| [ProjectE](https://modrinth.com/mod/projecte) | 1.1.0+ | ✅ |
| [Applied Energistics 2](https://modrinth.com/mod/ae2) | 19.2.17+ | ❌ (任意) |
| [JEI](https://modrinth.com/mod/jei) | 19.27+ | ❌ (任意) |

### ✨ 主な機能

#### EMC概略図砲
- **FE動力**で概略図を超高速設置（1〜256 blocks/tick）
- **ProjectE EMC**を消費してブロックを自動生成
- **AE2 MEストレージ / チェスト**からのアイテム取り出し
- 複数の置換モード（固体ブロック保護、ブロックエンティティ保護等）
- 概略図再利用オプション
- 第2世代GUI（展開式設定タブ、スピードスライダー、情報パネル）

#### フィラーモード
- **7種のモード**: 埋め立て / 完全消去 / 撤去 / 壁 / タワー / 箱 / 円壁
- 範囲指定ボードで範囲を指定
- 撤去モードでのEMC変換 / ストレージ搬入対応
- 撤去時のストレージ満杯検知・自動停止

#### 空中設置杖
- FEを消費してフレームブロックを空中に設置
- **Shift+スクロール**: 設置距離を調整（1〜15ブロック）
- **Shift+ホイール押込み**: 距離をデフォルト(5)にリセット
- **Shift+右クリック**: 設置した全ブロックを一括撤去

#### 範囲指定ボード
- 右クリックで2点を指定して範囲を設定
- 編集モードで個別の点を調整
- フィラーモード・撤去モードで使用

### 🔧 ビルド方法

```bash
gradlew.bat build
```

出力: `build/libs/emcschematicannon-x.x.x.jar`

### 📝 アップデート情報 (v1.1.0)
**"Gen2 GUI" Update**

> * **第2世代GUI**: 全面リニューアル。展開式の設定タブ・スピードタブ・情報タブを実装。
> * **フィラーモード**: 7種の建築パターン（埋め立て、壁、タワー、箱、円壁、消去、撤去）。
> * **空中設置杖**: FE駆動のフレームブロック空中設置ツール。距離調整・一括撤去対応。
> * **範囲指定ボード**: 2点指定による範囲設定ツール。編集モード搭載。
> * **撤去モード強化**: EMC変換 / ストレージ搬入の選択、満杯時の自動停止。
> * **プレビュー表示**: 概略図・フィラー両モードで範囲枠のプレビュー表示/非表示を切替可能。
> * **大砲アニメーション**: ターゲットへのスムーズな追従アニメーション（指数減衰補間）。

---

<a id="english"></a>
## 🇺🇸 English

A NeoForge mod that combines **Create**'s Schematic Cannon with **ProjectE**'s EMC system. With AE2 ME storage integration, it enables fully automated large-scale construction.

### 📦 Dependencies
| Mod | Version | Required |
|-----|---------|----------|
| [Create](https://modrinth.com/mod/create) | 6.0.9+ | ✅ |
| [ProjectE](https://modrinth.com/mod/projecte) | 1.1.0+ | ✅ |
| [Applied Energistics 2](https://modrinth.com/mod/ae2) | 19.2.17+ | ❌ (Optional) |
| [JEI](https://modrinth.com/mod/jei) | 19.27+ | ❌ (Optional) |

### ✨ Key Features

#### EMC Schematic Cannon
- **FE-powered** ultra-fast schematic placement (1–256 blocks/tick)
- Automatic block generation using **ProjectE EMC**
- Item extraction from **AE2 ME storage / chests**
- Multiple replace modes (solid block protection, block entity protection, etc.)
- Schematic reuse option
- Gen2 GUI (expandable settings tabs, speed slider, information panel)

#### Filler Mode
- **7 Modes**: Fill / Complete Erase / Removal / Wall / Tower / Box / Circle Wall
- Range specification using Range Board
- EMC conversion / storage insertion in removal mode
- Auto-pause when storage is full during removal

#### Air Placement Wand
- Place frame blocks in mid-air using FE energy
- **Shift+Scroll**: Adjust placement distance (1–15 blocks)
- **Shift+Middle Click**: Reset distance to default (5)
- **Shift+Right Click**: Remove all placed blocks at once

#### Range Board
- Right-click to set two points defining a range
- Edit mode for adjusting individual points
- Used with filler mode and removal mode

### 🔧 Build

```bash
gradlew.bat build
```

Output: `build/libs/emcschematicannon-x.x.x.jar`

### 📝 Update Notes (v1.1.0)
**"Gen2 GUI" Update**

> * **Gen2 GUI**: Complete overhaul with expandable settings, speed, and information tabs.
> * **Filler Mode**: 7 building patterns (Fill, Wall, Tower, Box, Circle Wall, Erase, Removal).
> * **Air Placement Wand**: FE-powered frame block placement tool with distance adjustment and bulk removal.
> * **Range Board**: Two-point range specification tool with edit mode.
> * **Enhanced Removal Mode**: Selectable EMC conversion / storage insertion, auto-pause when full.
> * **Preview Display**: Toggle range frame preview in both schematic and filler modes.
> * **Cannon Animation**: Smooth target-tracking animation using exponential decay interpolation.

---

### 🛠 Technology Stack / 技術スタック
- **Platform**: [NeoForge](https://neoforged.net/) 21.1.168+ (Minecraft 1.21.1)
- **EMC System**: [ProjectE](https://modrinth.com/mod/projecte) ITransmutationProxy / IEMCProxy
- **Storage**: [Applied Energistics 2](https://modrinth.com/mod/ae2) Grid Node Integration
- **Recipe Viewer**: [JEI](https://modrinth.com/mod/jei) Plugin Support

### 📄 License / ライセンス
All Rights Reserved
