package bo.edu.ucb.encuentra.core.domain.model

/** Las 6 categorías de la grilla "¿Qué perdiste?" del Figma. */
enum class Category(val label: String) {
    ELECTRONICA("Electrónica"),
    ROPA_ACCESORIOS("Ropa/Accesorios"),
    DOCUMENTOS("Documentos"),
    LIBROS_CUADERNOS("Libros/Cuadernos"),
    LLAVES("Llaves"),
    OTROS("Otros");

    val subcategories: List<Subcategory> get() = Subcategory.entries.filter { it.category == this }
}

/**
 * Subcategoría obligatoria del wizard (L02): la confirmación del mockup muestra "Electrónica / Laptops".
 * M01 la usa en el filtro duro de coincidencias.
 */
enum class Subcategory(val category: Category, val label: String) {
    LAPTOP(Category.ELECTRONICA, "Laptops"),
    CELULAR(Category.ELECTRONICA, "Celulares"),
    CALCULADORA(Category.ELECTRONICA, "Calculadoras"),
    AUDIFONOS(Category.ELECTRONICA, "Audífonos"),
    CARGADOR(Category.ELECTRONICA, "Cargadores"),
    TABLET(Category.ELECTRONICA, "Tablets"),

    MOCHILA(Category.ROPA_ACCESORIOS, "Mochilas"),
    CHAMARRA(Category.ROPA_ACCESORIOS, "Chamarras/Abrigos"),
    LENTES(Category.ROPA_ACCESORIOS, "Lentes"),
    BILLETERA(Category.ROPA_ACCESORIOS, "Billeteras"),
    TERMO(Category.ROPA_ACCESORIOS, "Termos/Botellas"),
    GORRA(Category.ROPA_ACCESORIOS, "Gorras"),

    CARNET_UNIVERSITARIO(Category.DOCUMENTOS, "Carnet universitario"),
    CEDULA(Category.DOCUMENTOS, "Cédula de identidad"),
    TARJETA_BANCARIA(Category.DOCUMENTOS, "Tarjetas bancarias"),
    OTRO_DOCUMENTO(Category.DOCUMENTOS, "Otros documentos"),

    LIBRO(Category.LIBROS_CUADERNOS, "Libros"),
    CUADERNO(Category.LIBROS_CUADERNOS, "Cuadernos"),
    APUNTES(Category.LIBROS_CUADERNOS, "Apuntes/Fotocopias"),

    LLAVES_CASA(Category.LLAVES, "Llaves de casa"),
    LLAVES_VEHICULO(Category.LLAVES, "Llaves de vehículo"),
    LLAVERO(Category.LLAVES, "Llaveros"),

    PARAGUAS(Category.OTROS, "Paraguas"),
    OTRO(Category.OTROS, "Otro")
}
