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
  <img src="https://img.shields.io/badge/license-MIT-blue.svg" alt="License">
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

### ✨ 追加アイテム・ブロック

#### 🔫 EMC概略図砲（ブロック）
CreateのSchematic CannonとProjectEのEMCを統合した全自動建築マシンです。

| 項目 | 詳細 |
|------|------|
| エネルギー容量 | 100,000 FE |
| 設置コスト | 500 FE / ブロック |
| 設置速度 | 1〜256 blocks/tick（スライダーで調整） |

**機能:**
- 概略図スロットに概略図を入れて開始ボタンで自動建築
- **EMCブロック生成**: ProjectEのEMCを消費してブロックを自動生成。手持ちのアイテムが不要
- **AE2 MEストレージ連携**: AE2ケーブルを隣接接続するとMEネットワークからアイテムを自動取り出し
- **チェスト連携**: 隣接するチェストからアイテムを取り出し
- **ストレージモード切替**: AE2+チェスト / AE2のみ / チェストのみ の3モード
- **置換モード**: 固体ブロック置換しない / 固体→固体 / 固体→任意 / 固体→空気
- **ブロックエンティティ保護**: チェスト等のBEを上書きしない保護機能
- **不足ブロックスキップ**: 在庫切れブロックを飛ばして続行
- **概略図再利用**: 完了後に概略図を消費しないオプション

**第2世代GUI:**
- 展開式の設定タブ（歯車アイコン）— 6つの設定ボタンを2×3グリッドに配置
- 展開式のスピードタブ（稲妻アイコン）— テクスチャベースのスライダーで速度調整
- 展開式の情報タブ（左側）— MOD説明テキスト、スクロール対応
- ブロックリスト — 4列×13行、マウスホイールスクロール、EMCアイコン表示

---

#### 🪄 空中設置杖（アイテム）
FEエネルギーを消費して、空中にフレームブロックを設置できる杖です。

| 項目 | 詳細 |
|------|------|
| エネルギー容量 | 400,000 FE |
| 設置コスト | 1,000 FE / ブロック |
| 最大設置数 | 400個（満充電時） |
| 設置距離 | 1〜15ブロック（デフォルト: 5） |

**操作方法:**
| 操作 | 動作 |
|------|------|
| 右クリック（空中） | 視線方向の設定距離にフレームブロックを設置 |
| 右クリック（ブロック面） | クリックした面の隣にフレームブロックを設置 |
| Shift + 右クリック | この杖で設置した全フレームブロックを一括撤去 |
| Shift + スクロール | 設置距離を調整（1〜15ブロック） |
| Shift + ホイール押込み | 設置距離をデフォルト(5)にリセット |

- エネルギーバーがアイテム上に表示（赤→緑のグラデーション）
- クリエイティブモードではエネルギー消費なし
- 設置したブロックの位置はアイテムに記録され、一括撤去に使用

---

#### 📋 範囲指定ボード（アイテム）
2点を指定して3D範囲を設定するツールです。フィラーモードや撤去モードで使用します。

| 項目 | 詳細 |
|------|------|
| レイキャスト距離 | 最大64ブロック |
| モード数 | 3（通常 / Pos1編集 / Pos2編集） |

**操作方法:**
| 操作 | 動作 |
|------|------|
| 右クリック（ブロック） | 座標を設定（通常モード: Pos1→Pos2交互） |
| 右クリック（空中） | 64ブロック先までレイキャストして座標設定 |
| Shift + 右クリック | 座標をクリア（モードにより対象が異なる） |
| Alt + スクロール | モード切替（通常 ↔ 編集） |
| Shift + スクロール（編集モード） | 編集対象を切替（Pos1 / Pos2） |

**モード詳細:**
- **通常モード**: 右クリックでPos1→Pos2を交互に設定。Shift+右クリックで両方クリア
- **Pos1編集モード**: 右クリックで常にPos1のみ設定。Shift+右クリックでPos1のみクリア
- **Pos2編集モード**: 右クリックで常にPos2のみ設定。Shift+右クリックでPos2のみクリア

---

#### 🧱 フレームブロック（ブロック）
空中設置杖で設置される透明な足場ブロックです。

| 項目 | 詳細 |
|------|------|
| 硬度 | 0.0（素手で即破壊） |
| 光透過 | あり（空を透過） |
| 窒息 | なし |
| 効果音 | 足場（Scaffolding） |
| ピストン | 押すと破壊 |

- 松明やレッドストーンを設置可能
- 視界を遮らない透明ブロック
- 建築の足場として使用

---

### 🔧 フィラーモード

EMC概略図砲を**フィラーモード**に切り替えると、範囲指定ボードで指定した範囲に対して以下の操作が可能です:

| モード | 説明 |
|--------|------|
| 埋め立て | 範囲内の空気ブロックを指定ブロックで埋める |
| 完全消去 | 範囲内の全ブロックを空気に置換 |
| 撤去 | 範囲内のブロックを撤去し、EMC変換またはストレージに搬入 |
| 壁 | 範囲の外壁のみを作成 |
| タワー | 範囲内に柱を作成 |
| 箱 | 範囲の外殻（6面）を作成 |
| 円壁 | 範囲内に円筒形の壁を作成 |

**撤去モードの詳細:**
- **EMC変換ON**: EMC値を持つブロックはProjectEのEMCに自動変換
- **EMC変換OFF**: 全てのブロックをストレージ（AE2/チェスト）に搬入
- ストレージが満杯になると自動的に作業を一時停止

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

### ✨ Added Items & Blocks

#### 🔫 EMC Schematic Cannon (Block)
An all-in-one automated building machine that integrates Create's Schematic Cannon with ProjectE's EMC.

| Spec | Detail |
|------|--------|
| Energy Capacity | 100,000 FE |
| Placement Cost | 500 FE / block |
| Placement Speed | 1–256 blocks/tick (adjustable via slider) |

**Features:**
- Insert a schematic and press Start for automated building
- **EMC Block Generation**: Automatically generates blocks by consuming ProjectE EMC — no items needed
- **AE2 ME Storage Integration**: Connect AE2 cables to pull items from ME network automatically
- **Chest Integration**: Extracts items from adjacent chests
- **Storage Mode Toggle**: AE2+Chest / AE2 Only / Chest Only
- **Replace Modes**: Don't Replace Solid / Solid→Solid / Solid→Any / Solid→Air
- **Block Entity Protection**: Prevents overwriting chests and other block entities
- **Skip Missing Blocks**: Continue placement even when out of stock
- **Schematic Reuse**: Option to keep the schematic after completion

**Gen2 GUI:**
- Expandable Settings tab (gear icon) — 6 setting buttons in a 2×3 grid
- Expandable Speed tab (lightning icon) — texture-based slider for speed adjustment
- Expandable Information tab (left side) — mod description text, scrollable
- Block list — 4 columns × 13 rows, mouse wheel scrolling, EMC icon overlay

---

#### 🪄 Air Placement Wand (Item)
A wand that consumes FE energy to place Frame Blocks in mid-air.

| Spec | Detail |
|------|--------|
| Energy Capacity | 400,000 FE |
| Placement Cost | 1,000 FE / block |
| Max Placements | 400 (at full charge) |
| Placement Distance | 1–15 blocks (default: 5) |

**Controls:**
| Input | Action |
|-------|--------|
| Right-click (air) | Place Frame Block at set distance in look direction |
| Right-click (block face) | Place Frame Block adjacent to clicked face |
| Shift + Right-click | Remove all Frame Blocks placed by this wand |
| Shift + Scroll | Adjust placement distance (1–15 blocks) |
| Shift + Middle Click | Reset distance to default (5) |

- Energy bar displayed on item (red → green gradient)
- No energy cost in Creative mode
- Placed block positions are stored in item data for bulk removal

---

#### 📋 Range Board (Item)
A tool for defining 3D ranges by specifying two corner points. Used with Filler Mode and Removal Mode.

| Spec | Detail |
|------|--------|
| Raycast Distance | Up to 64 blocks |
| Modes | 3 (Normal / Edit Pos1 / Edit Pos2) |

**Controls:**
| Input | Action |
|-------|--------|
| Right-click (block) | Set coordinate (Normal mode: alternates Pos1→Pos2) |
| Right-click (air) | Raycast up to 64 blocks to set coordinate |
| Shift + Right-click | Clear coordinates (target depends on mode) |
| Alt + Scroll | Toggle mode (Normal ↔ Edit) |
| Shift + Scroll (Edit mode) | Switch edit target (Pos1 / Pos2) |

**Mode Details:**
- **Normal Mode**: Right-click alternates between setting Pos1 and Pos2. Shift+Right-click clears both
- **Edit Pos1 Mode**: Right-click always sets Pos1 only. Shift+Right-click clears Pos1 only
- **Edit Pos2 Mode**: Right-click always sets Pos2 only. Shift+Right-click clears Pos2 only

---

#### 🧱 Frame Block (Block)
A transparent scaffolding block placed by the Air Placement Wand.

| Spec | Detail |
|------|--------|
| Hardness | 0.0 (instant break by hand) |
| Light Transmission | Yes (propagates skylight) |
| Suffocation | No |
| Sound | Scaffolding |
| Piston | Destroyed when pushed |

- Supports placement of torches and redstone
- Does not block vision — fully transparent
- Used as temporary scaffolding for construction

---

### 🔧 Filler Mode

When the EMC Schematic Cannon is switched to **Filler Mode**, the following operations can be performed on the range specified by the Range Board:

| Mode | Description |
|------|-------------|
| Fill | Fill air blocks within range with specified block |
| Complete Erase | Replace all blocks within range with air |
| Removal | Remove blocks and convert to EMC or insert into storage |
| Wall | Create only the outer walls of the range |
| Tower | Create pillars within the range |
| Box | Create the outer shell (6 faces) of the range |
| Circle Wall | Create a cylindrical wall within the range |

**Removal Mode Details:**
- **EMC Conversion ON**: Blocks with EMC values are automatically converted to ProjectE EMC
- **EMC Conversion OFF**: All blocks are inserted into storage (AE2/Chest)
- Work automatically pauses when storage is full

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
[MIT License](LICENSE)
