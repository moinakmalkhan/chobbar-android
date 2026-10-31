package shop.chobbar.app.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.navigation.NavHostController
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import shop.chobbar.app.BuildConfig
import shop.chobbar.app.R
import shop.chobbar.app.data.model.EmailBody
import shop.chobbar.app.data.model.RegisterBody
import shop.chobbar.app.ui.components.LoadingButton
import shop.chobbar.app.ui.components.LogoMark
import shop.chobbar.app.ui.nav.UiMessages
import shop.chobbar.app.ui.nav.fieldErrors
import shop.chobbar.app.ui.nav.userMessage
import shop.chobbar.app.ui.theme.LocalAppContainer
import shop.chobbar.app.util.Pk

/** Gmail sign-in via Credential Manager: Google ID token → /auth/login/google/ → our JWT pair. */
suspend fun googleIdToken(context: Context): String? {
    if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) {
        UiMessages.show(context.getString(R.string.auth_google_not_configured)); return null
    }
    val option = GetGoogleIdOption.Builder()
        .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
        .setFilterByAuthorizedAccounts(false)
        .setAutoSelectEnabled(true)
        .build()
    return try {
        val result = CredentialManager.create(context).getCredential(context, GetCredentialRequest.Builder().addCredentialOption(option).build())
        val cred = result.credential
        if (cred is CustomCredential && cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) GoogleIdTokenCredential.createFrom(cred.data).idToken else null
    } catch (_: GetCredentialCancellationException) {
        null
    } catch (e: Exception) {
        UiMessages.show(context.getString(R.string.app_google_failed)); null
    }
}

@Composable
private fun AuthFrame(nav: NavHostController, title: Int, subtitle: Int?, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize()) {
        BackTopBar("", nav)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            LogoMark(Modifier.width(56.dp).align(Alignment.CenterHorizontally))
            Text(stringResource(title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.align(Alignment.CenterHorizontally))
            subtitle?.let { Text(stringResource(it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterHorizontally)) }
            Spacer(Modifier.height(4.dp))
            content()
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GoogleButton(onToken: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    OutlinedButton(
        onClick = { busy = true; scope.launch { googleIdToken(ctx)?.let(onToken); busy = false } },
        enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp), shape = CircleShape,
    ) {
        Text("G  ", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.auth_continue_google))
    }
}

@Composable
private fun OrDivider() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f)); Text("  " + stringResource(R.string.auth_or_email) + "  ", style = MaterialTheme.typography.bodySmall); HorizontalDivider(Modifier.weight(1f))
    }
}

@Composable
fun PasswordField(value: String, onChange: (String) -> Unit, label: String, error: String? = null, hint: String? = null) {
    var show by remember { mutableStateOf(false) }
    OutlinedTextField(
        value, onChange, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true, isError = error != null,
        supportingText = (error ?: hint)?.let { { Text(it) } }, shape = RoundedCornerShape(14.dp),
        visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = { TextButton(onClick = { show = !show }) { Text(stringResource(if (show) R.string.auth_hide_password else R.string.auth_show_password), style = MaterialTheme.typography.labelSmall) } },
    )
}

@Composable
fun LoginScreen(nav: NavHostController) {
    val c = LocalAppContainer.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var errors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    fun done(name: String) { UiMessages.show(ctx.getString(R.string.auth_welcome, name)); nav.popBackStack() }
    AuthFrame(nav, R.string.auth_welcome_back, R.string.auth_sign_in_subtitle) {
        GoogleButton { token -> scope.launch { runCatching { c.auth.google(token) }.onSuccess { done(it.firstName) }.onFailure { UiMessages.show(it.userMessage(ctx)) } } }
        OrDivider()
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.auth_email)) }, singleLine = true, isError = errors["email"] != null, supportingText = errors["email"]?.let { { Text(it) } }, shape = RoundedCornerShape(14.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        PasswordField(password, { password = it }, stringResource(R.string.auth_password), errors["password"])
        TextButton(onClick = { nav.navigate("forgot") }, Modifier.align(Alignment.End)) { Text(stringResource(R.string.auth_forgot)) }
        LoadingButton(stringResource(R.string.auth_sign_in), busy, {
            if (!Pk.validEmail(email)) { errors = mapOf("email" to ctx.getString(R.string.auth_invalid_email)); return@LoadingButton }
            busy = true
            scope.launch {
                runCatching { c.auth.login(email, password) }.onSuccess { done(it.firstName) }.onFailure { errors = it.fieldErrors(); UiMessages.show(it.userMessage(ctx)) }
                busy = false
            }
        }, Modifier.fillMaxWidth(), enabled = password.isNotEmpty())
        Row(Modifier.align(Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.auth_no_account)); TextButton(onClick = { nav.navigate("register") }) { Text(stringResource(R.string.auth_create_account)) }
        }
    }
}

@Composable
fun RegisterScreen(nav: NavHostController) {
    val c = LocalAppContainer.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var first by rememberSaveable { mutableStateOf("") }
    var last by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var marketing by rememberSaveable { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var errors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    fun done(name: String) { UiMessages.show(ctx.getString(R.string.auth_welcome, name)); nav.popBackStack("account", inclusive = false) || nav.popBackStack() }
    AuthFrame(nav, R.string.auth_create_title, R.string.auth_create_subtitle) {
        GoogleButton { token -> scope.launch { runCatching { c.auth.google(token) }.onSuccess { done(it.firstName) }.onFailure { UiMessages.show(it.userMessage(ctx)) } } }
        OrDivider()
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(first, { first = it }, Modifier.weight(1f), label = { Text(stringResource(R.string.auth_first_name)) }, singleLine = true, isError = errors["first_name"] != null, shape = RoundedCornerShape(14.dp))
            OutlinedTextField(last, { last = it }, Modifier.weight(1f), label = { Text(stringResource(R.string.auth_last_name)) }, singleLine = true, shape = RoundedCornerShape(14.dp))
        }
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.auth_email)) }, singleLine = true, isError = errors["email"] != null, supportingText = errors["email"]?.let { { Text(it) } }, shape = RoundedCornerShape(14.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.auth_phone)) }, singleLine = true, isError = errors["phone"] != null, supportingText = errors["phone"]?.let { { Text(it) } }, shape = RoundedCornerShape(14.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
        PasswordField(password, { password = it }, stringResource(R.string.auth_password), errors["password"], stringResource(R.string.auth_password_hint))
        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(marketing, { marketing = it }); Text(stringResource(R.string.auth_marketing), style = MaterialTheme.typography.bodyMedium) }
        LoadingButton(stringResource(R.string.auth_create_account), busy, {
            val e = mutableMapOf<String, String>()
            if (first.isBlank()) e["first_name"] = ctx.getString(R.string.common_required)
            if (!Pk.validEmail(email)) e["email"] = ctx.getString(R.string.auth_invalid_email)
            if (phone.isNotBlank() && !Pk.validPhone(phone)) e["phone"] = ctx.getString(R.string.checkout_invalid_phone)
            if (password.length < 10) e["password"] = ctx.getString(R.string.auth_min_password)
            errors = e
            if (e.isNotEmpty()) return@LoadingButton
            busy = true
            scope.launch {
                runCatching { c.auth.register(RegisterBody(email.trim(), password, first.trim(), last.trim(), phone.trim(), marketing)) }
                    .onSuccess { done(it.firstName) }.onFailure { errors = it.fieldErrors(); UiMessages.show(it.userMessage(ctx)) }
                busy = false
            }
        }, Modifier.fillMaxWidth())
        Text(stringResource(R.string.auth_terms), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ForgotPasswordScreen(nav: NavHostController) {
    val c = LocalAppContainer.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var email by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var sent by rememberSaveable { mutableStateOf(false) }
    AuthFrame(nav, R.string.auth_reset_title, R.string.auth_reset_subtitle) {
        if (sent) Text(stringResource(R.string.auth_link_sent), style = MaterialTheme.typography.bodyLarge)
        else {
            OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.auth_email)) }, singleLine = true, shape = RoundedCornerShape(14.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
            LoadingButton(stringResource(R.string.auth_send_link), busy, {
                busy = true
                scope.launch { runCatching { c.api.requestReset(EmailBody(email.trim())) }.onSuccess { sent = true }.onFailure { UiMessages.show(it.userMessage(ctx)) }; busy = false }
            }, Modifier.fillMaxWidth(), enabled = Pk.validEmail(email))
        }
    }
}
