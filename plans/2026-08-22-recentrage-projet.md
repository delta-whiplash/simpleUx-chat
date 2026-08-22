# Recentrage du projet — 2026-08-22

**Contexte :** SimpleUX Chat est un side-project. Le fork ambitionne une refonte
UI/UX (« Luxury Mineral / Haute Horlogerie ») sur `apps/multiplatform/` en
préservant intégralement le cœur cryptographique et le protocole SimpleX. Un
premier audit (`2026-08-15-ux-audit-backlog.md`, 26 constats) avait déjà
identifié les problèmes ci-dessous ; la plupart des issues correspondantes
étaient fermées sur GitHub sans que le code ait changé. Ce document reprend le
plan appliqué pour recentrer le projet sur ce qui le rend maintenable, en
priorité, avant toute nouvelle fonctionnalité.

## Principe directeur

> Tout changement UX vit dans `views/ux/` + les tokens de thème. Toucher un
> fichier upstream demande une justification explicite.

## Constat qui a réordonné les priorités

Le workflow `build.yml` existant ne couvre pas `apps/multiplatform/**` (son
filtre `paths` liste uniquement `src/**`, les apps Haskell, `tests/**`...). Tout
le travail du fork — 100 % du code Kotlin/Compose modifié — n'était donc build
ni testé nulle part. Le badge « Tests 100% Passing » du README ne reposait sur
rien. C'est la cause racine du manque de maintenabilité, plus que le volume de
code : rien ne contraint les régressions à être visibles.

## Actions appliquées dans cette session

1. **CI dédiée au fork** (`.github/workflows/simpleux.yml`) : build de l'APK
   FOSS debug + tests desktop/common sur chaque PR touchant
   `apps/multiplatform/**`, avec deux lints bloquants (pas de hex bruts, pas de
   `= ChatModel` par défaut dans `views/ux/`).
2. **Suppression de la dette de confiance** :
   - Retrait complet de `sampleDirectoryGroups` / `PublicDirectorySearchResultsSection`
     (faux annuaire de groupes injecté dans la recherche, deux points d'appel
     dans `ChatListView.kt` et `NewChatSheet.kt`).
   - `ServerRadarSheet` : les libellés laissaient croire à un diagnostic live
     par serveur alors que les valeurs sont des faits statiques du protocole ;
     reformulés pour être honnêtes (« garanties du protocole, pas un contrôle
     en direct de vos serveurs »).
3. **Réduction de la dérive upstream** : `oneHandUI` remis à sa valeur par
   défaut upstream (`true`) dans `SimpleXAPI.kt` — le fork l'avait basculée à
   `false` sans retirer le contrôle utilisateur correspondant dans
   `Appearance.kt`, privant silencieusement tout le monde de ce réglage sur
   des dizaines d'écrans upstream (`TagListView`, `ChatView`, `NewChatSheet`,
   `GroupChatInfoView`, etc.).
4. **Garde-fous documentés** : `views/ux/README.md` fixe les règles de couche
   (pas d'édition d'upstream en place, pas de singleton `ChatModel`, pas de
   donnée fabriquée, tokens obligatoires, une feature à la fois).
5. **Badge README corrigé** pour pointer vers la vraie CI au lieu d'une
   affirmation invérifiée.

## Suite (session du même jour) : extraction partielle du shell (#04)

`TelegramTopHeader`, `TelegramBottomIslandBar`, `SimpleUxTab` et `IslandTabItem`
(457 lignes) ont été déplacés de `ChatListView.kt` vers
`views/ux/ChatListShell.kt`, avec un seul point d'appel conservé à chaque
endroit d'usage. Vérifié sans compilateur disponible (pas de SDK Android dans
cet environnement) par : extraction du bloc exact par plage de lignes (pas de
retype à la main — une première tentative de reconstitution manuelle du
contenu a été jetée après relecture car elle ne correspondait pas au code
réel), équilibrage accolades/parenthèses sur les trois fichiers touchés,
vérification qu'aucun symbole n'est déclaré deux fois, et que les arguments
nommés des points d'appel correspondent exactement aux signatures déplacées.

`ChatListView.kt` : 2127 → 1670 lignes. Le diff vs upstream reste à **525
lignes** (vs. la cible `< 120`) — la navigation par onglets elle-même
(`currentTab`, le `when` de routage, les filter pills, la gestion de l'état de
recherche) est encore intriquée dans le fichier upstream et n'a pas été
extraite dans cette passe. C'est un travail incrémental à poursuivre par
étapes similaires, chacune validée par la CI avant de continuer.

## Volontairement non fait dans cette session

- **Retrait des `chatModelInstance: ChatModel = ChatModel`** (issue #09,
  3 occurrences) : un refactor de signature sans compilateur pour vérifier
  est le genre d'erreur qui casse un side-project ; la CI ajoutée ci-dessus
  le validera dès la prochaine PR.
- **Tokenisation complète des couleurs / consolidation du glassmorphism**
  (issues #15–#17) : gros volume, à traiter par petites PR une fois la CI en
  place pour les valider.

## Décision de périmètre

Les 6 issues ouvertes `#46`–`#51` (pont IPC zero-copy, scanner QR natif SIMD,
tuning RTS GHC...) touchent le cœur Haskell / la couche FFI — **hors du tenet
« frontend-only » d'`AGENTS.md`**. Recommandation : les geler (`scope:deferred`)
tant que les points ci-dessus ne sont pas stabilisés. Un side-project qui mène
deux fronts (UX + perf native) en parallèle sans CI ni process est le scénario
qui part dans tous les sens.

## Prochaines étapes, dans l'ordre

1. Laisser la CI tourner sur ce commit/PR et corriger ce qu'elle trouve.
2. Extraire le shell `ChatListView.kt` (#04) — débloque tout le reste.
3. Retirer les singletons `ChatModel` dans `views/ux/` (#09).
4. Merge upstream (l'audit notait un chevauchement minimal au moment du fork).
5. Reprendre les features UX une par une, chacune conforme à
   `views/ux/README.md` avant de passer à la suivante.
