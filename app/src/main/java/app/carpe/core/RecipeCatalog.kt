package app.carpe.core

/** Small offline recipe starters. Ingredient text and searches stay on the device. */
data class LocalRecipe(
    val id: String,
    val title: String,
    val minutes: Int,
    /** 1 = budget-friendly staples; 2 = everyday ingredients. */
    val budgetTier: Int,
    val dietTags: Set<String>,
    val ingredients: List<String>,
    val steps: List<String>
)

object RecipeCatalog {
    val recipes = listOf(
        LocalRecipe("spinach_egg_rice", "Spinach and egg rice", 15, 1, setOf("vegetarian"),
            listOf("eggs", "cooked rice", "spinach", "garlic", "oil", "soy sauce"),
            listOf("Warm a little oil and cook the garlic briefly.", "Add spinach until wilted, then stir in cooked rice.", "Move the rice aside, scramble the eggs, then combine and season.")),
        LocalRecipe("black_bean_quesadilla", "Black bean quesadillas", 15, 1, setOf("vegetarian"),
            listOf("tortillas", "black beans", "cheese", "salsa"),
            listOf("Mash some beans with a fork and spread them over a tortilla.", "Add cheese and fold the tortilla.", "Toast in a dry pan until crisp and hot; serve with salsa.")),
        LocalRecipe("chickpea_tomato_bowl", "Chickpea and tomato rice bowl", 20, 1, setOf("vegan", "vegetarian", "gluten-free"),
            listOf("chickpeas", "canned tomatoes", "onion", "garlic", "cooked rice"),
            listOf("Cook chopped onion until soft, then add garlic.", "Stir in chickpeas and tomatoes; simmer until hot and thick.", "Serve over cooked rice and season to taste.")),
        LocalRecipe("tomato_garlic_pasta", "Tomato and garlic pasta", 20, 1, setOf("vegan", "vegetarian"),
            listOf("pasta", "tomatoes", "garlic", "olive oil", "basil"),
            listOf("Cook pasta according to its package.", "Warm garlic in olive oil, add chopped tomatoes, and cook until saucy.", "Toss with pasta and basil; add pasta water if needed.")),
        LocalRecipe("vegetable_omelet", "Quick vegetable omelet", 12, 1, setOf("vegetarian", "gluten-free"),
            listOf("eggs", "spinach", "bell pepper", "cheese"),
            listOf("Chop the vegetables and soften them in a lightly oiled pan.", "Pour in beaten eggs and cook gently until mostly set.", "Add cheese if wanted, fold, and cook until the center is done.")),
        LocalRecipe("lentil_tomato_soup", "Lentil and tomato soup", 35, 1, setOf("vegan", "vegetarian", "gluten-free"),
            listOf("lentils", "canned tomatoes", "carrot", "onion", "broth"),
            listOf("Soften chopped onion and carrot in a pot.", "Add rinsed lentils, tomatoes, and broth; bring to a boil.", "Simmer until lentils are tender, adding water if needed.")),
        LocalRecipe("banana_oats", "Banana peanut butter oats", 8, 1, setOf("vegan", "vegetarian"),
            listOf("oats", "banana", "peanut butter", "water or milk"),
            listOf("Simmer oats with water or milk until soft.", "Mash in half a banana.", "Top with peanut butter and the remaining banana.")),
        LocalRecipe("tuna_rice_bowl", "Tuna cucumber rice bowl", 10, 1, setOf("gluten-free"),
            listOf("canned tuna", "cooked rice", "cucumber", "lemon", "yogurt"),
            listOf("Drain tuna and flake it with a fork.", "Mix with lemon and a spoon of yogurt if you like.", "Serve over rice with chopped cucumber.")),
        LocalRecipe("tofu_vegetable_stirfry", "Tofu and vegetable stir-fry", 20, 2, setOf("vegan", "vegetarian"),
            listOf("tofu", "frozen vegetables", "rice", "soy sauce", "oil"),
            listOf("Pat tofu dry and cut it into cubes.", "Brown tofu in a hot pan with a little oil.", "Add vegetables and cook until hot; season with soy sauce and serve with rice.")),
        LocalRecipe("potato_bean_bowl", "Potato and bean bowl", 30, 1, setOf("vegan", "vegetarian", "gluten-free"),
            listOf("potatoes", "black beans", "corn", "salsa"),
            listOf("Dice potatoes and cook in a covered pan with a splash of water until tender.", "Warm beans and corn in the same pan.", "Serve with salsa and adjust seasoning.")),
        LocalRecipe("chickpea_salad", "Chickpea cucumber salad", 12, 1, setOf("vegan", "vegetarian", "gluten-free"),
            listOf("chickpeas", "cucumber", "tomato", "lemon", "olive oil"),
            listOf("Rinse chickpeas and chop cucumber and tomato.", "Toss with lemon juice, olive oil, salt, and pepper.", "Eat as-is or serve with bread or rice.")),
        LocalRecipe("peanut_noodles", "Quick peanut noodles", 15, 1, setOf("vegan", "vegetarian"),
            listOf("noodles", "peanut butter", "soy sauce", "lime", "frozen vegetables"),
            listOf("Cook noodles and reserve a little cooking water.", "Stir peanut butter, soy sauce, lime, and warm water into a sauce.", "Toss noodles and cooked vegetables with the sauce."))
    )

    fun search(query: String): List<LocalRecipe> {
        val clean = query.lowercase()
        val timeLimit = Regex("\\b(\\d{1,3})\\s*(?:minutes?|mins?)\\b").find(clean)?.groupValues?.get(1)?.toIntOrNull()
        val budgetOnly = listOf("budget", "cheap", "low cost", "low-cost", "inexpensive").any { term -> clean.contains(term) }
        val requestedTags = buildSet {
            if (clean.contains("vegan")) add("vegan")
            if (clean.contains("vegetarian")) add("vegetarian")
            if (clean.contains("gluten-free") || clean.contains("gluten free")) add("gluten-free")
        }
        val ignored = setOf(
            "a", "about", "and", "are", "for", "have", "help", "i", "in", "ingredients", "into",
            "me", "minutes", "min", "of", "on", "or", "quick", "recipe", "recipes", "some", "the",
            "to", "under", "within", "with", "want", "what", "vegan", "vegetarian", "gluten", "free",
            "budget", "cheap", "cost", "inexpensive", "low", "easy", "dinner", "meal"
        )
        val terms = WORD.findAll(clean).map { normalize(it.value) }.filter { it.length > 1 && it !in ignored }.toSet()
        return recipes.mapNotNull { recipe ->
            if (timeLimit != null && recipe.minutes > timeLimit) return@mapNotNull null
            if (budgetOnly && recipe.budgetTier > 1) return@mapNotNull null
            if (!recipe.dietTags.containsAll(requestedTags)) return@mapNotNull null
            val searchable = tokens(recipe.title + " " + recipe.ingredients.joinToString(" ") + " " + recipe.dietTags.joinToString(" "))
            val score = terms.count { it in searchable }
            if (terms.isNotEmpty() && score == 0) null else RankedRecipe(recipe, score)
        }.sortedWith(compareByDescending<RankedRecipe> { it.score }.thenBy { it.recipe.minutes }.thenBy { it.recipe.budgetTier })
            .map { it.recipe }
            .take(8)
    }

    private data class RankedRecipe(val recipe: LocalRecipe, val score: Int)
    private val WORD = Regex("[a-z][a-z'-]*")
    private fun tokens(text: String): Set<String> = WORD.findAll(text.lowercase()).map { normalize(it.value) }.toSet()
    private fun normalize(value: String): String = when {
        value.endsWith("ies") && value.length > 4 -> value.dropLast(3) + "y"
        value.endsWith("es") && value.length > 4 -> value.dropLast(2)
        value.endsWith("s") && value.length > 3 -> value.dropLast(1)
        else -> value
    }
}
