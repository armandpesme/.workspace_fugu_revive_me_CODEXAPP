# Spécifications Techniques : HUD "Downed" - Fugu’Dreams MMORPG
Ce document est une référence absolue pour le rendu programmatique (Java Renderer / `GuiGraphics`) de l'interface de mise à terre pour Minecraft 1.20.1 Forge.

---

## 1. Environnement de Rendu
*   **Technologie :** Custom Java Rendering (`GuiGraphics`, `PoseStack`).
*   **Résolution Virtuelle :** 1920x1080 (Scaling dynamique requis selon la taille de fenêtre).
*   **Performance :** Optimisé pour le rendu par tick (pas de chargement de texture disque en boucle).

## 2. Palette Chromatique (Format ARGB Java)
| Élément | Code Hex | Valeur Java (0xAARRGGBB) | Usage |
| :--- | :--- | :--- | :--- |
| **Fond HUD** | `#0A0D14` | `0xD90A0D14` | Fond principal (85% opacité) |
| **Accent Ambre** | `#FFB347` | `0xFFFFB347` | Titre, bordures, barre (État stable) |
| **Danger Orange**| `#FF6B00` | `0xFFFF6B00` | Barre de progression (Urgence moyenne) |
| **Critique Rouge**| `#FF0000` | `0xFFFF0000` | Bordures et barre (Urgence haute) |
| **Texte Secondaire**| `#E0E0E0` | `0xFFE0E0E0` | Sous-titres et informations |
| **Lueur Cyan** | `#00F2FF` | `0xFF00F2FF` | État de réanimation en cours |

---

## 3. Composants et Structure (Pixel-Perfect)

### A. Le Bloc Principal (Bottom-Center)
*   **Dimensions :** 520px (L) x 115px (H).
*   **Position :** Centré horizontalement, `y = screenHeight - 140px`.
*   **Rendu :**
    1.  `fill` : Rectangle de fond avec coins arrondis (Radius: 8px).
    2.  `stroke` : Ligne fine de 1px sur le contour.
    3.  `gradient` : Léger dégradé vertical du fond (`#161B26` en haut vers `#0A0D14` en bas).

### B. Titre et Sous-texte
*   **Titre "K.O." :** Font moderne, Style: Bold, Échelle: 2.0x. Centré à `y_start + 20px`.
*   **Sous-texte :** "Wait for an ally to revive you". Échelle: 1.0x. Centré sous le titre.

### C. Barre de Survie Dynamique (Timer : 35s)
*   **Conteneur :** 300px x 6px. Fond noir (`0xFF000000`).
*   **Progression :** Largeur = `(timer_restant / 35.0) * 300px`.
*   **Logique de Couleur :**
    *   `t > 20s` : `#FFB347` (Ambre)
    *   `10s < t <= 20s` : `#FF6B00` (Orange)
    *   `t <= 10s` : `#FF0000` (Rouge) + Animation de pulsation.

---

## 4. Systèmes d'Animations et Effets

### A. Vignette Post-Process
*   **Type :** Radial Gradient Overlay.
*   **Comportement :** Pulse au rythme du timer.
*   **Formule Alpha :** `alpha = base_alpha + (sin(ticks * 0.1) * intensity)`.
*   **Urgence :** L'intensité et la fréquence augmentent lorsque le timer passe sous les 10s.

### B. Feedback de Touche "Abandonner" [R]
*   **Action :** Maintenir [R] pendant 2.0 secondes.
*   **Visuel :** Un cercle de progression (jauge circulaire) se remplit autour du bouton en bas à droite.
*   **Couleur :** Blanche (`0xFFFFFFFF`) avec effet de bloom léger.

### C. Indicateur de Proximité Alliée
*   **Calcul de Distance :** `Math.sqrt(dx*dx + dy*dy + dz*dz)`.
*   **Flèche Directionnelle :** Triangle pivotant.
    *   Angle : `atan2(deltaZ, deltaX)` converti en degrés pour le `PoseStack`.

---

## 5. États de l'Interface (Variants)

### État "DERNIÈRE CHANCE"
*   **Modificateur :** Se déclenche si le joueur tombe pour la 2ème fois en 5 min.
*   **Visuel :** Bordure du bloc clignote en rouge vif. Titre devient "DERNIÈRE CHANCE".
*   **Pénalité :** Temps de réanimation doublé (8s au lieu de 4s).

### État "RÉANIMATION EN COURS"
*   **Modificateur :** Un allié a commencé l'interaction.
*   **Changement de Couleur :** Toute l'interface passe en mode "Cyan/Glassmorphism".
*   **Arc de Cercle :** Apparition d'un arc de progression de 360° au centre du HUD indiquant le temps de cast restant de l'allié.

---

## 6. Guide de Reproduction Code (Pseudo-Java)
```java
public void renderHUD(GuiGraphics graphics, float partialTicks) {
    long time = Util.getMillis();
    float pulse = (float) Math.sin(time / 500.0) * 0.5f + 0.5f;

    // 1. Rendu de la Vignette
    drawVignette(graphics, pulse);

    // 2. Rendu du Conteneur Principal
    drawRoundedBox(graphics, centerX - 260, height - 140, 520, 115, 0xD90A0D14);

    // 3. Rendu de la Barre (Lerp Color)
    int barColor = getDynamicColor(timer);
    drawProgressBar(graphics, centerX - 150, height - 80, 300, 6, barColor);
}
```
