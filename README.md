# Advanced Schematic Cannon

<p align="center">
  <img src="src/main/resources/icon.png" width="120" alt="Advanced Schematic Cannon">
</p>

<p align="center">
  <strong>Create × Applied Energistics 2 × ProjectE — Endgame Building Automation</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.1-62B47A.svg" alt="Minecraft">
  <img src="https://img.shields.io/badge/NeoForge-21.1.168+-orange.svg" alt="NeoForge">
  <img src="https://img.shields.io/badge/version-1.1.2-blue.svg" alt="Version">
  <img src="https://img.shields.io/badge/license-MIT-blue.svg" alt="License">
</p>

[日本語](#japanese) | [English](#english)

---

<a id="japanese"></a>
## 🇯🇵 日本語 (Japanese)

**Create** の概略図キャノンを高速化・自動化した **NeoForge Mod** です。Applied Energistics 2 の ME ネットワークから直接アイテムを取り出し、不足ブロックは AE2 自動クラフトに発注。ProjectE を導入すると EMC 錬成による無限建築も可能になります。マルチプレイ対応。

### 📦 依存Mod
| Mod | バージョン | 必須 |
|-----|-----------|------|
| [Create](https://modrinth.com/mod/create) | 6.0+ | ✅ |
| [NeoForge](https://neoforged.net/) | 21.1.168+ | ✅ |
| [Applied Energistics 2](https://modrinth.com/mod/ae2) | 19.0+ | ❌ (任意 / 強く推奨) |
| [ProjectE](https://www.curseforge.com/minecraft/mc-mods/projecte) | 1.0+ | ❌ (任意 / EMC型を追加) |
| [JEI](https://modrinth.com/mod/jei) | 19.27+ | ❌ (任意) |

### 📸 スクリーンショット

<p align="center">
  <img src="説明用素材/items.png" width="700" alt="追加アイテム一覧">
  <br><em>追加アイテム・ブロック一覧 — 強化型/EMC概略図砲、範囲指定ボード、空中設置杖、フレームブロック</em>
</p>

<p align="center">
  <img src="説明用素材/gui.png" width="700" alt="GUI">
  <br><em>第2世代GUI — 展開式の設定/速度/情報タブ、ブロックリスト、エネルギーバー</em>
</p>

### ✨ 追加アイテム・ブロック

#### 🔫 強化型概略図砲（ブロック）
高速建築の中核を担うエンドコンテンツ向けキャノンです。常時利用可能で ProjectE 不要。

| 項目 | 詳細 |
|------|------|
| エネルギー容量 | 100,000 FE |
| 設置コスト | 500 FE / ブロック |
| 設置速度 | 1〜256 blocks/tick（スライダーで調整） |
| マルチプレイ | オーナーUUIDロック、所有者のみ操作可（OPは許可） |

**機能:**
- 概略図スロットに概略図を入れて開始ボタンで自動建築
- **AE2 ME ストレージ連携**: 隣接 AE2 ケーブルから自動取り出し
- **AE2 自動クラフト**: 不足ブロックを AE2 のクラフト CPU へ発注（キャノン位置ごとに追跡、CPU 過負荷を防止）
- **AE2 ケーブル給電**: AE→FE 変換（最大 10,000 FE/tick）に対応
- **チェスト連携**: 隣接するチェストからアイテムを取り出し
- **ストレージモード切替**: AE2+チェスト / AE2のみ / チェストのみ の3モード
- **置換モード**: 固体ブロック置換しない / 固体→固体 / 固体→任意 / 固体→空気
- **ブロックエンティティ保護**: チェスト等のBEを上書きしない保護機能
- **不足ブロックスキップ**: 在庫切れブロックを飛ばして続行
- **概略図再利用**: 完了後に概略図を消費しないオプション

---

#### 🌌 EMC概略図砲（ブロック・ProjectE導入時のみ）
強化型のすべての機能に加えて、EMC による不足ブロック錬成に対応した上位バリアントです。

| 項目 | 詳細 |
|------|------|
| 登録条件 | ProjectE 導入時のみ自動登録 |
| 追加機能 | EMC 燃料スロット / EMC 錬成 / 撤去モードのEMC変換 |
| GUI | EMC モード ON/OFF トグル、EMC 残高表示、EMC 燃料アイコン |

**EMC関連機能:**
- **EMC 錬成**: 在庫切れ時に EMC を消費してブロックをその場で生成
- **EMC 燃料スロット**: アイテムを EMC に変換して内部バッファに蓄積（Ctrl+クリックで直接搬入）
- **撤去モード EMC 変換**: フィラー撤去時に EMC 価値のあるブロックを自動 EMC 化、それ以外はストレージへ

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

範囲指定ボードを挿入すると **フィラーモード** に切替わり、範囲内に対して以下の建築/撤去パターンを実行できます:

| モード | 説明 |
|--------|------|
| 埋め立て | 範囲内の空気ブロックを指定ブロックで埋める |
| 完全消去 | 範囲内の全ブロックを空気に置換 |
| 撤去 | 範囲内のブロックを撤去し、EMC変換またはストレージに搬入 |
| 壁 | 範囲の外壁のみを作成 |
| タワー | 範囲内に柱を作成 |
| 箱 | 範囲の外殻（6面）を作成 |
| 円壁 | 範囲内に円筒形の壁を作成 |

### 🔧 ビルド方法

```bash
gradlew.bat build
```

出力: `build/libs/advancedschematicannon-x.x.x.jar`

---

<a id="english"></a>
## 🇺🇸 English

A **NeoForge Mod** that supercharges and automates **Create**'s Schematic Cannon. Pulls items directly from your Applied Energistics 2 ME network, dispatches AE2 auto-crafting for missing blocks, and — with ProjectE installed — synthesizes blocks on the fly using EMC. Multiplayer compatible.

### 📦 Dependencies
| Mod | Version | Required |
|-----|---------|----------|
| [Create](https://modrinth.com/mod/create) | 6.0+ | ✅ |
| [NeoForge](https://neoforged.net/) | 21.1.168+ | ✅ |
| [Applied Energistics 2](https://modrinth.com/mod/ae2) | 19.0+ | ❌ (Optional / Highly recommended) |
| [ProjectE](https://www.curseforge.com/minecraft/mc-mods/projecte) | 1.0+ | ❌ (Optional / Adds the EMC variant) |
| [JEI](https://modrinth.com/mod/jei) | 19.27+ | ❌ (Optional) |

### 📸 Screenshots

<p align="center">
  <img src="説明用素材/items.png" width="700" alt="Added Items">
  <br><em>Added Items & Blocks — Enhanced/EMC Schematic Cannon, Range Board, Air Placement Wand, Frame Block</em>
</p>

<p align="center">
  <img src="説明用素材/gui.png" width="700" alt="GUI">
  <br><em>Gen2 GUI — Expandable Settings/Speed/Information tabs, block list, energy bar</em>
</p>

### ✨ Added Items & Blocks

#### 🔫 Enhanced Schematic Cannon (Block)
The core endgame builder. Always available — no ProjectE required.

| Spec | Detail |
|------|--------|
| Energy Capacity | 100,000 FE |
| Placement Cost | 500 FE / block |
| Placement Speed | 1–256 blocks/tick (adjustable via slider) |
| Multiplayer | Owner UUID locked; only the owner can operate (OPs allowed) |

**Features:**
- Insert a schematic and press Start for automated building
- **AE2 ME Storage Integration**: Connect AE2 cables to pull items from ME network automatically
- **AE2 Auto-Crafting**: Dispatches craft requests to your CPUs for missing blocks (per-cannon tracking prevents duplicate jobs)
- **AE2 Cable Power**: Converts AE→FE at up to 10,000 FE/tick
- **Chest Integration**: Extracts items from adjacent chests
- **Storage Mode Toggle**: AE2+Chest / AE2 Only / Chest Only
- **Replace Modes**: Don't Replace Solid / Solid→Solid / Solid→Any / Solid→Air
- **Block Entity Protection**: Prevents overwriting chests and other block entities
- **Skip Missing Blocks**: Continue placement even when out of stock
- **Schematic Reuse**: Option to keep the schematic after completion

---

#### 🌌 EMC Schematic Cannon (Block, requires ProjectE)
A premium variant of the Enhanced cannon that adds EMC-based block synthesis.

| Spec | Detail |
|------|--------|
| Registration | Auto-registered only when ProjectE is installed |
| Added Features | EMC fuel slot / EMC transmutation / Removal-to-EMC mode |
| GUI | EMC ON/OFF toggle, player EMC display, fuel slot icon |

**EMC features:**
- **EMC Transmutation**: Synthesizes missing blocks on the fly by consuming EMC
- **EMC Fuel Slot**: Convert items to internal EMC buffer (Ctrl+click to route items directly)
- **Removal Mode EMC**: Converts EMC-valued blocks to EMC during filler removal; non-EMC items go to storage

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

Insert a Range Board into the schematic slot to switch to **Filler Mode** and run the following patterns over the selected volume:

| Mode | Description |
|------|-------------|
| Fill | Fill air blocks within range with specified block |
| Complete Erase | Replace all blocks within range with air |
| Removal | Remove blocks and convert to EMC or insert into storage |
| Wall | Create only the outer walls of the range |
| Tower | Create pillars within the range |
| Box | Create the outer shell (6 faces) of the range |
| Circle Wall | Create a cylindrical wall within the range |

### 🔧 Build

```bash
gradlew.bat build
```

Output: `build/libs/advancedschematicannon-x.x.x.jar`

---

### 🛠 Technology Stack / 技術スタック
- **Platform**: [NeoForge](https://neoforged.net/) 21.1.168+ (Minecraft 1.21.1)
- **Building Engine**: [Create](https://modrinth.com/mod/create) Schematic API (data components, structure NBT)
- **Storage / Auto-Crafting**: [Applied Energistics 2](https://modrinth.com/mod/ae2) Grid Node + Crafting Service integration
- **EMC System (optional)**: [ProjectE](https://www.curseforge.com/minecraft/mc-mods/projecte) IEMCProxy / ITransmutationProxy via runtime bridge
- **Recipe Viewer**: [JEI](https://modrinth.com/mod/jei) Plugin Support

### 📝 Release Notes / リリースノート
See [`RELEASE_NOTES_v1.1.2.md`](RELEASE_NOTES_v1.1.2.md) and [`CHANGELOG.md`](CHANGELOG.md).

### 👤 Author / 作者
**BelugaLab**

### 📄 License / ライセンス
[MIT License](LICENSE)
