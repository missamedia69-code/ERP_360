package com.missa.b360.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.missa.b360.R
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.Red20
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/*
 * Kit de formulaires Missa Business 360 — une seule grammaire visuelle pour tous
 * les formulaires de l'application :
 *
 *  • en-tête clair (titre + sous-titre) ;
 *  • sections numérotées ① ② ③ à la couleur du module ;
 *  • champs pleine largeur, icône Phosphor NOIRE en tête, clavier adapté ;
 *  • champs courts regroupés deux par deux ([MissaRangee]) ;
 *  • choix exclusifs en tuiles ([MissaChoixTuiles]), listes en vrais menus déroulants ;
 *  • dates au calendrier (jamais tapées) ;
 *  • un seul bouton principal pleine largeur, désactivé tant que le requis manque ;
 *  • mention rassurante « données sur cet appareil » (offline-first).
 *
 * La couleur du module est transmise par [LocalCouleurFormulaire] : un formulaire
 * enveloppé dans [MissaFormulaireTheme] ou [MissaFormDialogue] n'a pas à la passer
 * à chaque champ.
 */

/** Couleur du module courant pour les formulaires (bordure active, pastilles, bouton). */
val LocalCouleurFormulaire = staticCompositionLocalOf { BrandBlue }

@Composable
fun MissaFormulaireTheme(couleur: Color, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalCouleurFormulaire provides couleur, content = content)
}

/** Texte lisible (blanc ou encre) posé sur un fond de la couleur donnée. */
fun Color.contenuLisible(): Color = if (luminance() > 0.40f) MissaInk else Color.White

/** Clavier et filtre de frappe d'un champ. */
enum class MissaClavier { TEXTE, ENTIER, DECIMAL, TELEPHONE, EMAIL, MOT_CLE }

private val FormatDate = "dd/MM/yyyy"
private const val MIDI_UTC = 43_200_000L

internal fun filtrerSaisie(clavier: MissaClavier, saisie: String): String = when (clavier) {
    MissaClavier.ENTIER -> saisie.filter(Char::isDigit)
    MissaClavier.DECIMAL -> {
        val brut = saisie.replace(',', '.').filter { it.isDigit() || it == '.' }
        val point = brut.indexOf('.')
        if (point < 0) brut else brut.substring(0, point + 1) + brut.substring(point + 1).replace(".", "")
    }
    MissaClavier.TELEPHONE -> saisie.filter { it.isDigit() || it == '+' || it == ' ' || it == '-' || it == '(' || it == ')' }
    MissaClavier.EMAIL -> saisie.trim()
    else -> saisie
}

@Composable
fun missaChampCouleurs(): TextFieldColors {
    val couleur = LocalCouleurFormulaire.current
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = couleur,
        unfocusedBorderColor = MissaBorder,
        disabledBorderColor = MissaBorder.copy(alpha = 0.6f),
        errorBorderColor = Red20,
        focusedLabelColor = MissaInk,
        unfocusedLabelColor = MissaMuted,
        cursorColor = MissaInk,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        disabledContainerColor = Color(0xFFF4F6FA),
        errorContainerColor = Color.White,
        focusedTextColor = MissaInk,
        unfocusedTextColor = MissaInk,
    )
}

private fun libelleAvecRequis(libelle: String, requis: Boolean): String =
    if (requis && !libelle.trimEnd().endsWith("*")) "$libelle *" else libelle

// ---------------------------------------------------------------------------
// Champs
// ---------------------------------------------------------------------------

/**
 * Champ texte standard. `icone` = drawable Phosphor (`Iv.X`) toujours teinté en noir.
 * Le filtre de frappe suit [clavier] : un champ DECIMAL n'accepte que chiffres et un
 * séparateur (la virgule est convertie en point, lisible par `toDoubleOrNull`).
 */
@Composable
fun MissaChampTexte(
    valeur: String,
    onValeur: (String) -> Unit,
    libelle: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    icone: Int? = null,
    clavier: MissaClavier = MissaClavier.TEXTE,
    requis: Boolean = false,
    aide: String? = null,
    erreur: String? = null,
    placeholder: String? = null,
    lignes: Int = 1,
    enabled: Boolean = true,
    lectureSeule: Boolean = false,
    suffixe: String? = null,
    longueurMax: Int? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focus by interaction.collectIsFocusedAsState()
    val focusRequester = remember { FocusRequester() }
    CadreChamp(
        libelle = libelle,
        requis = requis,
        icone = icone,
        // Ne pas afficher une erreur pendant que l’utilisateur est encore en train
        // de compléter une valeur intermédiaire (ex. « 12. » ou une adresse e-mail).
        erreur = erreur?.takeUnless { focus },
        aide = aide,
        enabled = enabled,
        actif = focus,
        suffixe = suffixe,
        modifier = modifier.clickable(
            enabled = enabled && !lectureSeule,
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
        ) { focusRequester.requestFocus() },
    ) {
        Box(contentAlignment = Alignment.CenterStart) {
            if (valeur.isEmpty() && placeholder != null) {
                Text(placeholder, style = StyleValeur, color = MissaMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            BasicTextField(
                value = valeur,
                onValueChange = { saisie ->
                    val filtre = filtrerSaisie(clavier, saisie)
                    onValeur(if (longueurMax != null) filtre.take(longueurMax) else filtre)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .semantics { contentDescription = libelle },
                enabled = enabled,
                readOnly = lectureSeule,
                textStyle = StyleValeur.copy(color = if (enabled) MissaInk else MissaMuted),
                singleLine = lignes <= 1,
                minLines = if (lignes <= 1) 1 else lignes,
                maxLines = if (lignes <= 1) 1 else lignes + 3,
                keyboardOptions = KeyboardOptions(
                    keyboardType = when (clavier) {
                        MissaClavier.ENTIER -> KeyboardType.Number
                        MissaClavier.DECIMAL -> KeyboardType.Decimal
                        MissaClavier.TELEPHONE -> KeyboardType.Phone
                        MissaClavier.EMAIL -> KeyboardType.Email
                        MissaClavier.MOT_CLE -> KeyboardType.Ascii
                        MissaClavier.TEXTE -> KeyboardType.Text
                    },
                    capitalization = if (clavier == MissaClavier.TEXTE) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
                ),
                interactionSource = interaction,
                cursorBrush = SolidColor(MissaInk),
            )
        }
    }
}

/** Hauteur d'un champ compact (libellé au-dessus + valeur) — structure matricielle. */
private val HauteurChamp = 44.dp
private val FormeChamp = RoundedCornerShape(10.dp)
private val StyleValeur = TextStyle(fontSize = 13.sp, lineHeight = 17.sp)

/**
 * Cadre commun à tous les champs du kit : icône noire, petit libellé TOUJOURS visible
 * au-dessus de la valeur (pas de libellé flottant qui réclame de la hauteur), suffixe
 * et icône de fin facultatifs. ~44 dp de haut contre 56 dp pour un OutlinedTextField.
 */
@Composable
private fun CadreChamp(
    libelle: String,
    requis: Boolean,
    icone: Int?,
    erreur: String?,
    aide: String?,
    enabled: Boolean,
    actif: Boolean,
    modifier: Modifier,
    suffixe: String? = null,
    iconeFin: Int? = null,
    contenu: @Composable () -> Unit,
) {
    val couleur = LocalCouleurFormulaire.current
    val bord = when {
        erreur != null -> Red20
        actif -> couleur
        else -> MissaBorder
    }
    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = HauteurChamp)
                .background(if (enabled) Color.White else Color(0xFFF4F6FA), FormeChamp)
                .border(if (actif || erreur != null) 1.5.dp else 1.dp, bord, FormeChamp)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icone != null) {
                Icon(painterResource(icone), contentDescription = null, tint = MissaInk, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    libelleAvecRequis(libelle, requis),
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    color = if (erreur != null) Red20 else MissaMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                contenu()
            }
            if (suffixe != null) {
                Spacer(Modifier.width(4.dp))
                Text(suffixe, fontSize = 12.sp, color = MissaMuted)
            }
            if (iconeFin != null) {
                Spacer(Modifier.width(6.dp))
                Icon(painterResource(iconeFin), contentDescription = null, tint = MissaInk, modifier = Modifier.size(16.dp))
            }
        }
        (erreur ?: aide)?.let { texte ->
            Text(
                texte,
                fontSize = 10.5.sp,
                lineHeight = 13.sp,
                color = if (erreur != null) Red20 else MissaMuted,
                modifier = Modifier.padding(start = 10.dp, top = 2.dp),
            )
        }
    }
}

/** Champ non éditable qui ouvre un sélecteur (liste, calendrier…) au toucher. */
@Composable
private fun ChampCliquable(
    valeur: String,
    libelle: String,
    icone: Int?,
    iconeFin: Int,
    requis: Boolean,
    enabled: Boolean,
    placeholder: String?,
    aide: String?,
    erreur: String?,
    modifier: Modifier,
    actif: Boolean = false,
    onClick: () -> Unit,
) {
    CadreChamp(
        libelle = libelle,
        requis = requis,
        icone = icone,
        erreur = erreur,
        aide = aide,
        enabled = enabled,
        actif = actif,
        iconeFin = iconeFin,
        modifier = modifier
            .semantics { contentDescription = libelle }
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Text(
            // Espace insécable si vide : la ligne garde sa hauteur (grille régulière).
            valeur.ifEmpty { placeholder ?: "\u00A0" },
            style = StyleValeur,
            color = if (valeur.isEmpty()) MissaMuted else MissaInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Liste de choix : vrai menu déroulant ancré au champ. `options` = valeur → libellé
 * lisible ; `actionNouveau` ajoute une entrée « + … » en tête (création rapide).
 */
@Composable
fun <T> MissaChampListe(
    libelle: String,
    options: List<Pair<T, String>>,
    selection: T?,
    onSelection: (T) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    icone: Int? = null,
    requis: Boolean = false,
    enabled: Boolean = true,
    placeholder: String? = null,
    aide: String? = null,
    erreur: String? = null,
    actionNouveau: Pair<String, () -> Unit>? = null,
) {
    var ouvert by remember { mutableStateOf(false) }
    val texte = options.firstOrNull { it.first == selection }?.second.orEmpty()
    Box(modifier) {
        ChampCliquable(
            valeur = texte,
            libelle = libelle,
            icone = icone,
            iconeFin = if (ouvert) Iv.ExpandLess else Iv.ExpandMore,
            requis = requis,
            enabled = enabled && (options.isNotEmpty() || actionNouveau != null),
            placeholder = placeholder,
            aide = aide,
            erreur = erreur,
            actif = ouvert,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (options.isEmpty() && actionNouveau != null) actionNouveau.second() else ouvert = true
            },
        )
        MissaMenuDeroulant(
            expanded = ouvert,
            onDismissRequest = { ouvert = false },
            modifier = Modifier.widthIn(min = 220.dp),
        ) {
            if (actionNouveau != null) {
                DropdownMenuItem(
                    text = { Text("+ ${actionNouveau.first}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MissaInk) },
                    onClick = {
                        ouvert = false
                        actionNouveau.second()
                    },
                )
            }
            options.forEach { (valeur, libelleOption) ->
                DropdownMenuItem(
                    text = { Text(libelleOption, fontSize = 13.sp, color = MissaInk) },
                    trailingIcon = if (valeur == selection) {
                        { Icon(painterResource(Iv.Check), contentDescription = null, tint = MissaInk, modifier = Modifier.size(16.dp)) }
                    } else {
                        null
                    },
                    onClick = {
                        ouvert = false
                        onSelection(valeur)
                    },
                )
            }
        }
    }
}

/** Formate une date (millis) en jj/MM/aaaa dans le fuseau de l'appareil. */
fun formaterDateCourte(millis: Long): String = SimpleDateFormat(FormatDate, Locale.getDefault()).format(Date(millis))

/** Date au calendrier Material 3 (jamais saisie au clavier). Valeur en millis locales. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissaChampDate(
    valeur: Long?,
    onValeur: (Long?) -> Unit,
    libelle: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    requis: Boolean = false,
    enabled: Boolean = true,
    effacable: Boolean = !requis,
    aide: String? = null,
    erreur: String? = null,
    icone: Int? = Iv.Calendar,
) {
    var ouvert by remember { mutableStateOf(false) }
    ChampCliquable(
        valeur = valeur?.let(::formaterDateCourte).orEmpty(),
        libelle = libelle,
        icone = icone,
        iconeFin = Iv.ExpandMore,
        requis = requis,
        enabled = enabled,
        placeholder = null,
        aide = aide,
        erreur = erreur,
        modifier = modifier,
        onClick = { ouvert = true },
    )
    if (ouvert) {
        // Le calendrier travaille en UTC : minuit UTC ± fuseau peut changer de jour.
        // On passe par « midi UTC » du jour civil local pour un aller-retour exact.
        val initial = valeur?.let { versMidiUtc(it) }
        val etat = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { ouvert = false },
            confirmButton = {
                TextButton(onClick = {
                    etat.selectedDateMillis?.let { onValeur(depuisUtc(it, 12, 0)) }
                    ouvert = false
                }) { Text(stringResource(R.string.st_ok)) }
            },
            dismissButton = {
                Row {
                    if (effacable && valeur != null) {
                        TextButton(onClick = {
                            onValeur(null)
                            ouvert = false
                        }) { Text(stringResource(R.string.form_effacer)) }
                    }
                    TextButton(onClick = { ouvert = false }) { Text(stringResource(R.string.st_annuler)) }
                }
            },
        ) {
            DatePicker(state = etat)
        }
    }
}

/** Variante pour les écrans qui stockent la date en texte « jj/MM/aaaa ». */
@Composable
fun MissaChampDateTexte(
    valeur: String,
    onValeur: (String) -> Unit,
    libelle: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    requis: Boolean = false,
) {
    val format = remember { SimpleDateFormat(FormatDate, Locale.getDefault()) }
    val millis = remember(valeur) { runCatching { format.parse(valeur.trim())?.time }.getOrNull() }
    MissaChampDate(
        valeur = millis,
        onValeur = { choisi -> onValeur(choisi?.let { format.format(Date(it)) }.orEmpty()) },
        libelle = libelle,
        modifier = modifier,
        requis = requis,
    )
}

/** Date + heure : calendrier puis horloge 24 h. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissaChampDateHeure(
    valeur: Long?,
    onValeur: (Long) -> Unit,
    libelle: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    requis: Boolean = false,
    erreur: String? = null,
) {
    var etape by remember { mutableStateOf(0) } // 0 fermé · 1 date · 2 heure
    var jourUtc by remember { mutableStateOf<Long?>(null) }
    val format = remember { SimpleDateFormat("$FormatDate HH:mm", Locale.getDefault()) }
    ChampCliquable(
        valeur = valeur?.let { format.format(Date(it)) }.orEmpty(),
        libelle = libelle,
        icone = Iv.Schedule,
        iconeFin = Iv.ExpandMore,
        requis = requis,
        enabled = true,
        placeholder = null,
        aide = null,
        erreur = erreur,
        modifier = modifier,
        onClick = { etape = 1 },
    )
    if (etape == 1) {
        val etat = rememberDatePickerState(initialSelectedDateMillis = valeur?.let { versMidiUtc(it) })
        DatePickerDialog(
            onDismissRequest = { etape = 0 },
            confirmButton = {
                TextButton(
                    onClick = {
                        jourUtc = etat.selectedDateMillis
                        etape = if (jourUtc != null) 2 else 0
                    },
                ) { Text(stringResource(R.string.form_suivant)) }
            },
            dismissButton = { TextButton(onClick = { etape = 0 }) { Text(stringResource(R.string.st_annuler)) } },
        ) {
            DatePicker(state = etat)
        }
    }
    if (etape == 2) {
        val cal = Calendar.getInstance().apply { valeur?.let { timeInMillis = it } }
        val etatHeure = rememberTimePickerState(
            initialHour = if (valeur != null) cal.get(Calendar.HOUR_OF_DAY) else 8,
            initialMinute = if (valeur != null) cal.get(Calendar.MINUTE) else 0,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { etape = 0 },
            containerColor = Color.White,
            text = { TimePicker(state = etatHeure) },
            confirmButton = {
                TextButton(onClick = {
                    jourUtc?.let { onValeur(depuisUtc(it, etatHeure.hour, etatHeure.minute)) }
                    etape = 0
                }) { Text(stringResource(R.string.st_ok)) }
            },
            dismissButton = { TextButton(onClick = { etape = 0 }) { Text(stringResource(R.string.st_annuler)) } },
        )
    }
}

/** Jour civil local de `millis` → midi UTC de ce même jour (entrée du DatePicker). */
private fun versMidiUtc(millis: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = millis }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH), 12, 0, 0)
    }.timeInMillis
}

/** Jour sélectionné (UTC) + heure locale → millis locales. */
private fun depuisUtc(jourUtc: Long, heure: Int, minute: Int): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = jourUtc + 0L }
    return Calendar.getInstance().apply {
        clear()
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), heure, minute, 0)
    }.timeInMillis
}

// ---------------------------------------------------------------------------
// Choix
// ---------------------------------------------------------------------------

data class MissaTuile<T>(val valeur: T, val libelle: String, val icone: Int? = null)

/** Choix exclusif en tuiles (icône noire + libellé), la tuile active prend la couleur du module. */
@Composable
fun <T> MissaChoixTuiles(
    options: List<MissaTuile<T>>,
    selection: T?,
    onSelection: (T) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    colonnes: Int = 4,
    enabled: Boolean = true,
) {
    val couleur = LocalCouleurFormulaire.current
    // Les longs libellés ne sont jamais écrasés dans une grille de 3–4 colonnes :
    // deux colonnes gardent le bouton proche de son texte et lisible en traduction.
    val libelleLong = options.any { it.libelle.length > 14 || it.libelle.count { c -> c == ' ' } >= 2 }
    val parLigne = (if (libelleLong) minOf(colonnes, 2) else colonnes).coerceAtLeast(1)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.chunked(parLigne).forEach { ligne ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ligne.forEach { tuile ->
                    val active = tuile.valeur == selection
                    Surface(
                        onClick = { onSelection(tuile.valeur) },
                        enabled = enabled,
                        shape = RoundedCornerShape(10.dp),
                        color = if (active) couleur.copy(alpha = 0.12f).compositeOver(Color.White) else Color.White,
                        border = BorderStroke(if (active) 1.5.dp else 1.dp, if (active) couleur else MissaBorder),
                        modifier = Modifier.weight(1f).heightIn(min = if (tuile.icone != null) 50.dp else 38.dp),
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            tuile.icone?.let {
                                Icon(painterResource(it), contentDescription = null, tint = MissaInk, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(2.dp))
                            }
                            Text(
                                tuile.libelle,
                                fontSize = 11.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                color = MissaInk,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                repeat(parLigne - ligne.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun MissaCaseACocher(
    coche: Boolean,
    onChange: (Boolean) -> Unit,
    libelle: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    aide: String? = null,
    enabled: Boolean = true,
) {
    val couleur = LocalCouleurFormulaire.current
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(enabled = enabled) { onChange(!coche) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 32.dp) {
            Checkbox(
                checked = coche,
                onCheckedChange = null, // la ligne entière est l'unique cible : aucun double basculement
                enabled = enabled,
                modifier = Modifier.size(24.dp),
                colors = CheckboxDefaults.colors(checkedColor = couleur, checkmarkColor = couleur.contenuLisible(), uncheckedColor = MissaMuted),
            )
        }
        Spacer(Modifier.width(4.dp))
        Column(Modifier.weight(1f)) {
            Text(libelle, fontSize = 12.5.sp, lineHeight = 15.sp, color = MissaInk, maxLines = 2, overflow = TextOverflow.Ellipsis)
            aide?.let { Text(it, fontSize = 10.5.sp, color = MissaMuted) }
        }
    }
}

@Composable
fun MissaInterrupteur(
    actif: Boolean,
    onChange: (Boolean) -> Unit,
    libelle: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    aide: String? = null,
    enabled: Boolean = true,
) {
    val couleur = LocalCouleurFormulaire.current
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(enabled = enabled) { onChange(!actif) }
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(libelle, fontSize = 12.5.sp, lineHeight = 15.sp, color = MissaInk, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            aide?.let { Text(it, fontSize = 10.5.sp, color = MissaMuted) }
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = actif,
            onCheckedChange = null, // la ligne entière est la cible tactile, sans double déclenchement
            enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = couleur, checkedThumbColor = couleur.contenuLisible()),
        )
    }
}

// ---------------------------------------------------------------------------
// Structure
// ---------------------------------------------------------------------------

/** Titre de section numéroté (pastille ronde à la couleur du module). */
@Composable
fun MissaFormSectionTitre(
    titre: String,
    numero: Int? = null,
    sousTitre: String? = null,
    modifier: Modifier = Modifier.fillMaxWidth(),
    icone: Int? = null,
) {
    val couleur = LocalCouleurFormulaire.current
    Row(modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        when {
            numero != null -> Box(
                Modifier.size(20.dp).background(couleur, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("$numero", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = couleur.contenuLisible())
            }
            icone != null -> Icon(painterResource(icone), contentDescription = null, tint = MissaInk, modifier = Modifier.size(16.dp))
        }
        if (numero != null || icone != null) Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(titre, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = MissaInk)
            sousTitre?.let { Text(it, fontSize = 10.5.sp, color = MissaMuted) }
        }
    }
}

/** Section complète : titre numéroté + contenu espacé de 12 dp. */
@Composable
fun MissaFormSection(
    titre: String,
    numero: Int? = null,
    sousTitre: String? = null,
    modifier: Modifier = Modifier.fillMaxWidth(),
    icone: Int? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MissaFormSectionTitre(titre = titre, numero = numero, sousTitre = sousTitre, icone = icone)
        content()
    }
}

/** Deux (ou trois) champs courts côte à côte ; les enfants prennent `Modifier.weight(1f)`. */
@Composable
fun MissaRangee(
    modifier: Modifier = Modifier.fillMaxWidth(),
    content: @Composable RowScope.() -> Unit,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top, content = content)
}

/** Bouton principal pleine largeur, couleur du module ; grisé tant que le formulaire est incomplet. */
@Composable
fun MissaBoutonPrincipal(
    texte: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    enCours: Boolean = false,
    couleur: Color = LocalCouleurFormulaire.current,
) {
    val contenu = couleur.contenuLisible()
    Button(
        onClick = onClick,
        enabled = enabled && !enCours,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = couleur,
            contentColor = contenu,
            disabledContainerColor = Color(0xFFE3E8F1),
            disabledContentColor = MissaMuted,
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        modifier = modifier.heightIn(min = 44.dp),
    ) {
        if (enCours) {
            CircularProgressIndicator(color = contenu, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        } else {
            Text(texte, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Action secondaire (brouillon, précédent…) : contour neutre, même hauteur. */
@Composable
fun MissaBoutonSecondaire(
    texte: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MissaBorder),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        modifier = modifier.heightIn(min = 44.dp),
    ) {
        Text(texte, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MissaInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Mention rassurante sous le bouton : les données restent sur l'appareil. */
@Composable
fun MissaNoteSecurite(modifier: Modifier = Modifier.fillMaxWidth()) {
    Row(modifier, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(Iv.Lock), contentDescription = null, tint = MissaInk, modifier = Modifier.size(11.dp))
        Spacer(Modifier.width(4.dp))
        Text(stringResource(R.string.form_note_locale), fontSize = 10.sp, color = MissaMuted)
    }
}

/** Message d'erreur de formulaire (bandeau rouge discret). */
@Composable
fun MissaFormErreur(message: String?, modifier: Modifier = Modifier.fillMaxWidth()) {
    if (message.isNullOrBlank()) return
    Surface(color = Color(0xFFFFEEF0), shape = RoundedCornerShape(10.dp), modifier = modifier) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(Iv.Warning), contentDescription = null, tint = MissaInk, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(message, fontSize = 12.sp, color = Red20)
        }
    }
}

/**
 * Pied de formulaire plein écran : bouton principal (+ action secondaire éventuelle)
 * et mention de sécurité, sur une surface blanche détachée par une ombre.
 */
@Composable
fun MissaFormPied(
    texte: String,
    onValider: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    actif: Boolean = true,
    enCours: Boolean = false,
    secondaire: Pair<String, () -> Unit>? = null,
    secondaireActif: Boolean = true,
    erreur: String? = null,
) {
    Surface(color = Color.White, shadowElevation = 10.dp, modifier = modifier) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MissaFormErreur(erreur)
            if (secondaire != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MissaBoutonSecondaire(secondaire.first, secondaire.second, Modifier.weight(1f), enabled = secondaireActif && !enCours)
                    MissaBoutonPrincipal(texte, onValider, Modifier.weight(1f), enabled = actif, enCours = enCours)
                }
            } else {
                MissaBoutonPrincipal(texte, onValider, enabled = actif, enCours = enCours)
            }
            MissaNoteSecurite()
        }
    }
}

/**
 * Dialogue de formulaire : en-tête (icône, titre, sous-titre, fermer), contenu
 * défilant, bouton principal pleine largeur + « Annuler », mention de sécurité.
 *
 * Le contenu défile déjà : ne pas y remettre de `verticalScroll` ni de `LazyColumn`.
 */
@Composable
fun MissaFormDialogue(
    titre: String,
    onFermer: () -> Unit,
    libelleValider: String,
    onValider: () -> Unit,
    couleur: Color = LocalCouleurFormulaire.current,
    sousTitre: String? = null,
    icone: Int? = null,
    validerActif: Boolean = true,
    enCours: Boolean = false,
    libelleAnnuler: String? = null,
    erreur: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onFermer, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        MissaFormulaireTheme(couleur) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth(0.95f).widthIn(max = 560.dp),
            ) {
                Column {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (icone != null) {
                            Box(
                                Modifier.size(30.dp).background(couleur.copy(alpha = 0.18f).compositeOver(Color.White), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(painterResource(icone), contentDescription = null, tint = MissaInk, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(titre, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MissaInk, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            sousTitre?.let { Text(it, fontSize = 11.sp, color = MissaMuted) }
                        }
                        IconButton(onClick = onFermer) {
                            Icon(painterResource(Iv.Close), contentDescription = stringResource(R.string.st_annuler), tint = MissaInk, modifier = Modifier.size(20.dp))
                        }
                    }
                    HorizontalDivider(color = MissaBorder.copy(alpha = 0.6f))
                    Column(
                        Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        content = content,
                    )
                    Column(
                        Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        MissaFormErreur(erreur, Modifier.fillMaxWidth().padding(bottom = 4.dp))
                        // Annuler + Valider sur une seule rangée : une ligne de gagnée.
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MissaBoutonSecondaire(libelleAnnuler ?: stringResource(R.string.st_annuler), onFermer, Modifier.weight(1f))
                            MissaBoutonPrincipal(libelleValider, onValider, Modifier.weight(1.6f), enabled = validerActif, enCours = enCours)
                        }
                        MissaNoteSecurite()
                    }
                }
            }
        }
    }
}

/**
 * Icône déduite du libellé d'un moyen de paiement (espèces, mobile money, carte,
 * banque, chèque). Les moyens sont saisis librement par l'entreprise : on reconnaît
 * les mots courants dans les cinq langues, sinon icône « paiement » générique.
 */
fun iconeModePaiement(libelle: String): Int {
    val l = libelle.lowercase()
    return when {
        listOf("esp", "cash", "caisse", "efectivo", "نقد", "现金").any { it in l } -> Iv.Savings
        listOf("mobile", "momo", "orange", "mtn", "wave", "airtel", "moov", "m-pesa", "جوال", "手机").any { it in l } -> Iv.Smartphone
        listOf("carte", "card", "tarjeta", "visa", "master", "بطاقة", "卡").any { it in l } -> Iv.Payments
        listOf("chèque", "cheque", "check", "شيك", "支票").any { it in l } -> Iv.RequestQuote
        listOf("banq", "bank", "virement", "transfer", "banco", "بنك", "银行").any { it in l } -> Iv.Bank
        listOf("crédit", "credit", "terme", "مؤجل", "赊").any { it in l } -> Iv.Schedule
        else -> Iv.Payments
    }
}

/**
 * Choix d'un moyen de paiement « à la maquette » : tuiles icône + libellé tant qu'il
 * y a au plus 8 moyens, liste déroulante au-delà.
 */
@Composable
fun MissaChoixPaiement(
    moyens: List<String>,
    selection: String?,
    onSelection: (String) -> Unit,
    libelle: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
) {
    if (moyens.size in 1..8) {
        MissaChoixTuiles(
            options = moyens.map { MissaTuile(it, it, iconeModePaiement(it)) },
            selection = selection,
            onSelection = onSelection,
            modifier = modifier,
            colonnes = if (moyens.size == 3 || moyens.size > 4 && moyens.size != 8) 3 else minOf(4, moyens.size),
            enabled = enabled,
        )
    } else {
        MissaChampListe(
            libelle = libelle,
            options = moyens.map { it to it },
            selection = selection,
            onSelection = onSelection,
            modifier = modifier,
            icone = selection?.let { iconeModePaiement(it) } ?: Iv.Payments,
            enabled = enabled,
        )
    }
}
