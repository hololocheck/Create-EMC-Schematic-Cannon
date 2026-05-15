# Advanced Schematic Cannon — v1.1.2.1 Release Notes

**Release Date:** 2026-05-15
**Author:** BelugaLab
**Target:** Minecraft 1.21.1 / NeoForge 21.1.168+

[English](#english) | [日本語](#日本語)

---

## English

### Overview
v1.1.2.1 is a **critical bugfix & visual-consistency patch** on top of v1.1.2. The headline fix resolves a catastrophic interaction with ProjectE-less environments: the pickaxe block tag was being broken by an unconditional reference to a conditionally-registered block, which silently disabled the tier check for **every** pickaxe-mineable block in the game. A second held-item texture fix and metadata bump round out the release. No gameplay data migrations are required.

### 🚨 Critical Bug Fix

- **Tool mining speed broken without ProjectE** (PR #4 by @nekorobi-0) — `data/minecraft/tags/block/mineable/pickaxe.json` referenced `advancedschematicannon:emc_schematic_cannon` as a **required** entry. Because the EMC block is only registered when ProjectE is installed, the tag failed to resolve in ProjectE-less environments. Minecraft's tag loader discards a whole tag when it contains an unresolved required entry — so the entire `minecraft:mineable/pickaxe` tag (vanilla + every other mod's blocks) was being thrown away. The visible symptom was that diamond/iron pickaxes mined at bare-hand speed and tier checks stopped working; installing ProjectE made the problem disappear. The fix wraps the EMC entry with `{ "id": "...", "required": false }`, which the tag loader skips silently when the block is absent instead of dropping the whole tag.

### 🎨 Visual Fix

- **Enhanced cannon held-item texture** — The Enhanced Schematic Cannon's item model inherits from `block/emc_schematic_cannon/item.json`, whose texture key `#9` pointed at the EMC variant's `cannon_barrel_front` texture. As a result, holding the Enhanced variant displayed the EMC barrel front in the player's hand, inventory, and dropped item. The item model now overrides only `#9` with `cannon_barrel_front_enhanced`, keeping the shared geometry/textures intact so future changes to the base item model still apply to both variants.

### 🧾 Metadata & Polish

- **mod_version** — Bumped to **1.1.2.1** in `gradle.properties`.
- **In-Game Version Display** — `neoforge.mods.toml` `version` field synced to `"1.1.2.1"` (still hard-coded to bypass the `${file.jarVersion}` 0.0NONE bug from v1.1.2).
- **README restyle** — Repository README rewritten to match the SpatialAudioSystem layout: centered logo, tagline, shield badge row, JP/EN language toggle, Spec/Detail tables, unified Technology Stack footer.
- **Indentation cleanup** — `pickaxe.json` indentation normalized to 2-space after the PR merge (cosmetic only; no functional change).
- **CHANGELOG** — `CHANGELOG.md` gains a v1.1.2.1 section.
- **MODRINTH_DESCRIPTION.md** — New bilingual project-page description added for Modrinth use.

### 📦 Dependencies

| Mod | Version | Required |
|-----|---------|----------|
| Create | 6.0+ | ✅ Required |
| Applied Energistics 2 | 19.0+ | ⚙️ Optional (highly recommended for AE2 features) |
| ProjectE | 1.0+ | ⚙️ Optional (enables EMC variant) |
| JEI | 19.27+ | ⚙️ Optional |

### 🔄 Upgrade Notes

- **Drop-in upgrade** from v1.1.2. Worlds, schematics, range boards, and recipes are preserved.
- Players who were running v1.1.2 **without ProjectE** should upgrade immediately — every pickaxe in the world has been operating with broken tier behavior.
- No configuration changes required.

### 🙏 Credits

- **@nekorobi-0** (PR #4) — Reported and fixed the pickaxe-tag breakage with the canonical `required: false` pattern.

---

## 日本語

### 概要
v1.1.2.1 は v1.1.2 への **致命バグ修正 & ビジュアル整合パッチ** です。本リリースの目玉は、ProjectE 非導入環境で発生していた **致命的タグ破壊バグ** の修正です: 条件付き登録ブロックを必須エントリで参照していたため pickaxe タグ全体が破壊され、**ゲーム内の全 pickaxe 採掘可能ブロック**の tier 判定が無効化されていました。加えて手持ちアイテムのテクスチャ修正とメタ情報更新を含みます。ゲームデータの移行は不要です。

### 🚨 致命バグ修正

- **ProjectE 非導入時にツール採掘速度が破綻する不具合**(PR #4 / @nekorobi-0 様)— `data/minecraft/tags/block/mineable/pickaxe.json` が条件付き登録ブロック `advancedschematicannon:emc_schematic_cannon` を **必須エントリ** で参照していました。EMC ブロックは ProjectE 導入時のみ登録されるため、ProjectE 不在環境ではタグエントリの解決に失敗します。Minecraft のタグローダーは必須エントリ解決失敗時にタグ全体を破棄するため、`minecraft:mineable/pickaxe` タグそのもの(バニラ + 他 MOD の全ブロック)が消滅していました。症状はダイヤ/鉄ピッケルなどが素手と同じ採掘速度で動作し、tier 判定が機能しないというもの。ProjectE を入れると正常化していたのもこのため。修正は EMC エントリを `{ "id": "...", "required": false }` でラップするもので、未登録時は警告ログのみで安全にスキップされタグ全体は維持されます。

### 🎨 ビジュアル修正

- **強化型概略図砲の手持ちテクスチャ修正** — 強化型のアイテムモデルが EMC 型の `block/emc_schematic_cannon/item.json` を親として継承しており、texture key `#9` が EMC 型の `cannon_barrel_front` を指していたため、強化型を手に持つ・インベントリに入れる・地面にドロップした場合に EMC 型の砲身テクスチャが表示されていました。アイテムモデルで `#9` のみを `cannon_barrel_front_enhanced` にオーバーライドする方式に変更。本体形状や他のテクスチャは EMC 型と共有し続けるため、将来の基本モデル変更も両バリアントに自動反映されます。

### 🧾 メタ情報 & 整備

- **mod_version** — `gradle.properties` を **1.1.2.1** に更新。
- **ゲーム内バージョン表示** — `neoforge.mods.toml` の `version` フィールドも `"1.1.2.1"` に同期(`${file.jarVersion}` が `0.0NONE` を返す v1.1.2 で対応した直接指定方式を維持)。
- **README デザイン刷新** — リポジトリ README を SpatialAudioSystem と同じレイアウトに統一(中央寄せロゴ、タグライン、シールドバッジ行、JP/EN 切替、Spec/Detail テーブル、共通の Technology Stack フッター)。
- **インデント整形** — PR マージ後の `pickaxe.json` のインデントを 2 スペースに正規化(機能影響なし)。
- **CHANGELOG** — `CHANGELOG.md` に v1.1.2.1 セクションを追加。
- **MODRINTH_DESCRIPTION.md** — Modrinth プロジェクトページ用の英日 2 言語説明 md を新規追加。

### 📦 依存 Mod

| Mod | バージョン | 必須 |
|-----|-----------|------|
| Create | 6.0+ | ✅ 必須 |
| Applied Energistics 2 | 19.0+ | ⚙️ 任意(AE2 機能利用に強く推奨) |
| ProjectE | 1.0+ | ⚙️ 任意(EMC バリアント有効化) |
| JEI | 19.27+ | ⚙️ 任意 |

### 🔄 アップグレード手順

- **v1.1.2 から drop-in アップグレード可能**。ワールド・概略図・範囲指定ボード・レシピは保持されます。
- v1.1.2 を **ProjectE 無し** で使っていた方は **早急にアップグレード推奨**。ワールド内の全ピッケルが tier 判定無効状態で動作していた状況が解消されます。
- 設定変更不要。

### 🙏 クレジット

- **@nekorobi-0 様**(PR #4)— pickaxe タグ破壊バグを発見し、Minecraft 公式の `required: false` パターンによる正規修正を提案・実装してくださいました。
