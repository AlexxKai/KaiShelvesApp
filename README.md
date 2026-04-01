# KaiShelvesApp
Aplicación para gestión de listas de libros.


app/src/main/java/com/example/kaishelvesapp/
├── MainActivity.kt
├── data
│   ├── model
│   │   ├── Usuario.kt
│   │   ├── Libro.kt
│   │   └── LibroLeido.kt
│   └── repository
│       ├── AuthRepository.kt
│       └── BookRepository.kt
├── ui
│   ├── navigation
│   │   └── AppNavigation.kt
│   ├── screen
│   │   ├── login
│   │   │   └── LoginScreen.kt
│   │   ├── register
│   │   │   └── RegisterScreen.kt
│   │   └── home
│   │       └── HomeScreen.kt
│   ├── theme
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   └── viewmodel
│       └── AuthViewModel.kt