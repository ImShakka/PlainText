package com.example.plaintext

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.plaintext.ui.theme.PlainTextTheme
import kotlinx.serialization.Serializable
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.Cursor
import android.database.SQLException

@Serializable
object ScreenLogin

@Serializable
object ScreenList

@Serializable
data class ScreenEdit(val passwordId: Int = -1)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PlainTextTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = ScreenLogin // define a tela inicial como Login
                ) {
                    // rota para a tela de Login
                    composable<ScreenLogin> {
                        // passa o navController para a tela de Login para que ela possa navegar
                        Login_screen(navController)
                    }
                    // rota para a tela de lista de senha
                    composable<ScreenList> {
                        ListScreen(navController)
                    }
                    // rota para a tela de edicao e adicao da senha
                    composable<ScreenEdit> { backStackEntry ->
                        val args = backStackEntry.toRoute<ScreenEdit>()
                        EditScreen(navController, args.passwordId)
                    }
                }
            }
        }
    }
}

@Serializable
data class Password(
    var id: Int,
    val name: String,
    val login: String,
    val password: String,
    val notes: String
)

class Database(context: android.content.Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_VERSION = 5
        const val DATABASE_NAME = "PlainText.db"
        private const val SQL_CREATE_PASS = "CREATE TABLE passwords (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT, " +
                "login TEXT, " +
                "password TEXT, " +
                "notes TEXT)"

        private const val SQL_POPULATE_PASS = "INSERT INTO passwords VALUES " +
                "(NULL, 'Steam', 'shakka', 'Teste123', 'Nota de Teste')"

        private const val SQL_DELETE_PASS = "DROP TABLE IF EXISTS passwords"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_PASS)
        db.execSQL(SQL_POPULATE_PASS)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(SQL_DELETE_PASS)
        onCreate(db)
    }
}

class PasswordDAO(private val context: android.content.Context) {

    private val database: SQLiteDatabase

    init {
        database = Database(context).writableDatabase
    }

    fun getList(): ArrayList<Password> {
        val result = ArrayList<Password>()
        val sql = "SELECT id, name, login, password, notes FROM passwords ORDER BY name"
        var cursor: Cursor? = null

        try {
            cursor = database.rawQuery(sql, null)
            while (cursor.moveToNext()) {
                val id = cursor.getInt(0)
                val name = cursor.getString(1)
                val login = cursor.getString(2)
                val password = cursor.getString(3)
                val notes = cursor.getString(4)
                result.add(Password(id, name, login, password, notes))
            }
        } catch (e: SQLException) {
            Toast.makeText(context, "Erro ao obter a lista: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            cursor?.close()
        }
        return result
    }

    fun add(password: Password): Boolean {
        val sql = "INSERT INTO passwords VALUES (NULL, " +
                "'${password.name}', " +
                "'${password.login}', " +
                "'${password.password}', " +
                "'${password.notes}')"

        return try {
            database.execSQL(sql)
            Toast.makeText(context, "Senha salva!", Toast.LENGTH_SHORT).show()
            true
        } catch (e: SQLException) {
            Toast.makeText(context, "Erro! ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun update(password: Password): Boolean {
        val sql = "UPDATE passwords SET " +
                "name='${password.name}', " +
                "login='${password.login}', " +
                "password='${password.password}', " +
                "notes='${password.notes}' " +
                "WHERE id=${password.id}"

        return try {
            database.execSQL(sql)
            Toast.makeText(context, "Senha atualizada!", Toast.LENGTH_SHORT).show()
            true
        } catch (e: SQLException) {
            Toast.makeText(context, "Erro! ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun get(id: Int): Password? {
        val sql = "SELECT id, name, login, password, notes FROM passwords WHERE id=$id"
        var cursor: Cursor? = null
        var password: Password? = null

        try {
            cursor = database.rawQuery(sql, null)
            if (cursor.moveToNext()) {
                val foundId = cursor.getInt(0)
                val name = cursor.getString(1)
                val login = cursor.getString(2)
                val passwordStr = cursor.getString(3)
                val notes = cursor.getString(4)
                password = Password(foundId, name, login, passwordStr, notes)
            }
        } catch (e: SQLException) {
            Toast.makeText(context, "Erro ao obter a senha: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            cursor?.close()
        }
        return password
    }
}

// componente para a barra superior (TopAppBar)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBarComponent() {
    // estado para controlar a visibilidade do menu dropdown
    var expanded by remember { mutableStateOf(false) }
    val shouldShowDialog = remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text("PlainText") }, // titulo da barra superior
        actions = {
            // botao de icone para abrir o menu dropdown
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Menu")
            }
            // menu dropdown que aparece quando o ícone eh clicado
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false } // Fecha o menu ao clicar fora
            ) {
                // otem do menu para "Configuracoes"
                DropdownMenuItem(
                    text = { Text("Configurações") },
                    onClick = {
                        // acao ao clicar em Configuracoes
                        expanded = false // Fecha o menu
                        println("Configurações clicado!")
                    },
                    modifier = Modifier.padding(8.dp)
                )
                // item do menu para "Sobre"
                DropdownMenuItem(
                    text = { Text("Sobre") },
                    onClick = {
                        // acao ao clicar em Sobre
                        expanded = false // fecha o menu
                        shouldShowDialog.value = true
                        // println("Sobre clicado!")
                    },
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    )

    if (shouldShowDialog.value) {
        MyAlertDialog(shouldShowDialog = shouldShowDialog)
    }
}

@Composable
fun MyAlertDialog(shouldShowDialog: MutableState<Boolean>) {
    if (shouldShowDialog.value) {
        AlertDialog(
            onDismissRequest = {
                shouldShowDialog.value = false
            },
            title = { Text(text = "Sobre") },
            text = { Text(text = "PlainText Password Manager v1.0") },
            confirmButton = {
                Button(
                    onClick = {
                        shouldShowDialog.value = false
                    }
                ) {
                    Text(text = "OK")
                }
            }
        )
    }
}

// componente para o conteudo principal da tela de login
@SuppressLint("RestrictedApi")
@Composable
fun Login_screen(navController: NavController, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var loginText by remember { mutableStateOf("") }
    var passwordText by remember { mutableStateOf("") }
    var saveLoginInfo by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopBarComponent() } // define a barra superior para a tela de Login
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // bloco superior verde com imagem e texto
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color(0xFF8BC34A)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color.Transparent, RoundedCornerShape(40.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "Android Logo",
                        modifier = Modifier.size(80.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "\"The most secure password manager\"",
                    color = Color.White,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Bob and Alice",
                    color = Color.White,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Digite suas credenciais para continuar",
                fontSize = 16.sp,
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = loginText,
                onValueChange = { loginText = it },
                label = { Text("Login:") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = passwordText,
                onValueChange = { passwordText = it },
                label = { Text("Senha:") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = saveLoginInfo,
                    onCheckedChange = { saveLoginInfo = it }
                )
                Text(text = "Salvar as informações de login", color = Color.Black)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // botao "Enviar"modificado para navegacao
            Button(
                onClick = {
                    if (loginText.isBlank()) {
                        Toast.makeText(context, "Insira um login", Toast.LENGTH_SHORT).show()
                    } else {
                        // navega para a tela da lista apos o login
                        navController.navigate(ScreenList) {
                            // limpa a pilha de volta para que o usuario nao possa voltar para a tela de login
                            popUpTo(ScreenLogin::class) {
                                inclusive = true
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(2.dp, Color.Black),
                colors = ButtonDefaults.elevatedButtonColors(),
                elevation = ButtonDefaults.buttonElevation(8.dp)
            ) {
                Text("Entrar", fontSize = 18.sp, modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}

@Composable
fun ListScreen(navController: NavController) {
    val context = LocalContext.current
    val passwordDAO = remember { PasswordDAO(context) }
    val passwords = remember { mutableStateListOf<Password>() }

    LaunchedEffect(Unit) {
        passwords.addAll(passwordDAO.getList())
    }

    Scaffold(
        topBar = { TopBarComponent() },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // navega para a tela de edicao para adicionar uma nova senha (passwordId = -1)
                    navController.navigate(ScreenEdit(passwordId = -1))
                },
                modifier = Modifier.padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, "Adicionar nova senha")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(passwords) { password ->
                    PasswordItem(password = password) { clickedPasswordId ->
                        // a clicar em um item, navega para a tela de edicao com o ID da senha
                        navController.navigate(ScreenEdit(passwordId = clickedPasswordId))
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        val lifecycleObserver = object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onResume(owner: androidx.lifecycle.LifecycleOwner) {
                super.onResume(owner)
                passwords.clear()
                passwords.addAll(passwordDAO.getList())
            }
        }
        (context as? ComponentActivity)?.lifecycle?.addObserver(lifecycleObserver)
        onDispose {
            (context as? ComponentActivity)?.lifecycle?.removeObserver(lifecycleObserver)
        }
    }
}

@Composable
fun PasswordItem(password: Password, onClick: (Int) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick(password.id) },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // icone da chave (ic_item_key)
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(Color.Transparent, RoundedCornerShape(25.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "\uD83D\uDD11", fontSize = 30.sp) // emoji de chave
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = password.name,
                    fontSize = 20.sp, //
                    color = Color.Black
                )
                Text(
                    text = password.login,
                    fontSize = 14.sp, //
                    color = Color.Gray
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight, // icone de seta para a direita
                contentDescription = "Detalhes",
                tint = Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun EditScreen(navController: NavController, passwordId: Int) {
    val context = LocalContext.current
    val passwordDAO = remember { PasswordDAO(context) }

    // estados para os campos de entrada
    var name by remember { mutableStateOf("") }
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // carrega os dados da senha se for uma edicao
    LaunchedEffect(passwordId) {
        if (passwordId != -1) { // se passwordId for diferente de -1, é uma edicao
            val existingPassword = passwordDAO.get(passwordId)
            existingPassword?.let {
                name = it.name
                login = it.login
                password = it.password
                notes = it.notes
            }
        }
    }

    Scaffold(
        topBar = { TopBarComponent() }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (passwordId == -1) "Adicionar Nova Senha" else "Editar Senha",
                fontSize = 24.sp,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                )
            )
            OutlinedTextField(
                value = login,
                onValueChange = { login = it },
                label = { Text("Login") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                )
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Senha") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                )
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notas") },
                modifier = Modifier.fillMaxWidth().height(120.dp).padding(vertical = 8.dp),
                singleLine = false,
                maxLines = 5,
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // botao Salvar
            Button(
                onClick = {
                    val newPassword = Password(passwordId, name, login, password, notes)
                    val result = if (passwordId == -1) {
                        passwordDAO.add(newPassword)
                    } else {
                        passwordDAO.update(newPassword)
                    }

                    if (result) {
                        navController.popBackStack() // volta para a tela lista
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(2.dp, Color.Blue),
                colors = ButtonDefaults.elevatedButtonColors(),
                elevation = ButtonDefaults.buttonElevation(8.dp)
            ) {
                Text("SALVAR", fontSize = 18.sp, modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}

//@Composable
//fun Hello_screen(args: ScreenHello) {
//    Scaffold(
//        topBar = { TopBarComponent() } // Adiciona a TopBar também na tela de boas-vindas
//    ) { paddingValues ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(paddingValues)
//                .background(Color.White)
//                .padding(16.dp),
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.Center
//        ) {
//            Text(
//                text = "Olá ${args.name}!",
//                fontSize = 24.sp,
//                color = Color.Black,
//                textAlign = TextAlign.Center
//            )
//        }
//    }
//}

@Preview(showBackground = true, name = "LoginScreen", widthDp = 320)
@Composable
fun PreviewLoginVertical() {
    PlainTextTheme {
        val navController = rememberNavController() // Mock NavController para preview
        Login_screen(navController = navController)
    }
}

@Preview(showBackground = true, name = "ListScreen", widthDp = 320)
@Composable
fun PreviewListVertical() {
    PlainTextTheme {
        val navController = rememberNavController() // Mock NavController para preview
        ListScreen(navController = navController)
    }
}

@Preview(showBackground = true, name = "EditScreen", widthDp = 320)
@Composable
fun PreviewEditVertical() {
    PlainTextTheme {
        val navController = rememberNavController()
        EditScreen(navController = navController, passwordId = 0) // Previsualiza com um ID existente
    }
}