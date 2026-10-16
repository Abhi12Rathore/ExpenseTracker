package com.example.expensetrackerpro.domain.usecase.quickadd

import com.example.expensetrackerpro.domain.models.CategoryHint

/**
 * Every word quick add knows, and nothing else: the parsers hold the rules. Another language is
 * another instance of this, not other parsers.
 *
 * Words are lowercase and matched whole against the sentence's words, except a category keyword
 * longer than [shortKeywordLength], which is also found inside a word ("dmart" has "mart").
 */
data class QuickAddKeywords(
    /** Words that mark a number as the amount, before it or after it: "rs 450", "450 rupees". */
    val currencyWords: List<String>,
    /** What a number is multiplied by when this follows it, joined ("4.5k") or apart ("1.05 lakh"). */
    val multipliers: Map<String, Long>,
    /** Numbers written out: "one lakh five thousand". */
    val numberWords: Map<String, Long>,
    /** Joins written-out numbers and says nothing on its own: "one hundred and fifty". */
    val numberJoiners: List<String>,
    /** Days back from today. Phrases of several words are allowed: "day before yesterday". */
    val dayOffsets: Map<String, Int>,
    /** What comes before a weekday to mean the last one: "last monday". */
    val lastWords: List<String>,
    /** The seven weekdays, Sunday first, each with every form it is written in. */
    val weekdays: List<List<String>>,
    /** The twelve months, January first, each with every form it is written in. */
    val months: List<List<String>>,
    /** What follows a day of the month: "12th". */
    val ordinalSuffixes: List<String>,
    /** What follows a time of day, which is not an amount: "8pm", "9 am". */
    val timeSuffixes: List<String>,
    /** Money in: "salary", "received", "refund". */
    val incomeWords: List<String>,
    /** Money out: "spent", "paid". */
    val expenseWords: List<String>,
    /** Words that are neither the title nor the note: "spent", "on", "ka". */
    val fillerWords: List<String>,
    /** What starts the note after the title, besides a comma: "for dinner", "with Rahul". One word each. */
    val noteWords: List<String>,
    /** A word in the sentence, and the kind of spending or income it stands for. The first match wins. */
    val categoryKeywords: List<Pair<String, CategoryHint>>,
    /** Category keywords that name only the kind, not the place: left out of a title that has another word. */
    val genericWords: List<String>,
    val shortKeywordLength: Int,
    val titleMaxLength: Int,
    /**
     * Words between two transactions said in one go: "450 swiggy and 120 uber". Commas and new
     * lines split too. Not between written-out numbers, where "and" is a [numberJoiners] word.
     */
    val splitWords: List<String> = emptyList()
) {

    companion object {

        /** English as it is written in India, with the Hinglish people type into their phones. */
        val EN_IN = QuickAddKeywords(
            currencyWords = listOf("rs", "inr", "rupees", "rupee", "rupaye", "rupay", "rupiya", "rupiye", "bucks"),
            multipliers = mapOf(
                "hundred" to 100L, "sau" to 100L,
                "k" to 1_000L, "thousand" to 1_000L, "hazar" to 1_000L, "hazaar" to 1_000L, "hajar" to 1_000L,
                "l" to 100_000L, "lakh" to 100_000L, "lakhs" to 100_000L, "lac" to 100_000L,
                "lacs" to 100_000L, "lakh's" to 100_000L,
                "cr" to 10_000_000L, "crore" to 10_000_000L, "crores" to 10_000_000L
            ),
            numberWords = mapOf(
                "one" to 1L, "two" to 2L, "three" to 3L, "four" to 4L, "five" to 5L, "six" to 6L,
                "seven" to 7L, "eight" to 8L, "nine" to 9L, "ten" to 10L, "eleven" to 11L,
                "twelve" to 12L, "thirteen" to 13L, "fourteen" to 14L, "fifteen" to 15L,
                "sixteen" to 16L, "seventeen" to 17L, "eighteen" to 18L, "nineteen" to 19L,
                "twenty" to 20L, "thirty" to 30L, "forty" to 40L, "fifty" to 50L, "sixty" to 60L,
                "seventy" to 70L, "eighty" to 80L, "ninety" to 90L,
                "ek" to 1L, "do" to 2L, "teen" to 3L, "char" to 4L, "chaar" to 4L, "paanch" to 5L,
                "panch" to 5L, "das" to 10L, "bees" to 20L, "pachas" to 50L, "pachaas" to 50L
            ),
            numberJoiners = listOf("and"),
            dayOffsets = mapOf(
                "today" to 0, "aaj" to 0, "tonight" to 0, "this morning" to 0,
                "yesterday" to -1, "kal" to -1, "last night" to -1, "kal raat" to -1,
                "day before yesterday" to -2, "parso" to -2, "parson" to -2
            ),
            lastWords = listOf("last", "pichhle", "pichle"),
            weekdays = listOf(
                listOf("sunday"),
                listOf("monday", "mon"),
                listOf("tuesday", "tue", "tues"),
                listOf("wednesday", "wed"),
                listOf("thursday", "thu", "thur", "thurs"),
                listOf("friday", "fri"),
                listOf("saturday")
            ),
            months = listOf(
                listOf("jan", "january"), listOf("feb", "february"), listOf("mar", "march"),
                listOf("apr", "april"), listOf("may"), listOf("jun", "june"), listOf("jul", "july"),
                listOf("aug", "august"), listOf("sep", "sept", "september"), listOf("oct", "october"),
                listOf("nov", "november"), listOf("dec", "december")
            ),
            ordinalSuffixes = listOf("st", "nd", "rd", "th"),
            timeSuffixes = listOf("am", "pm", "baje", "bje"),
            incomeWords = listOf(
                "salary", "received", "receive", "got", "refund", "refunded", "cashback", "credited",
                "freelance", "interest", "earned", "income", "mila", "mile", "bonus", "stipend", "dividend"
            ),
            expenseWords = listOf("spent", "spend", "paid", "pay", "bought", "buy", "debited", "kharcha", "kharch"),
            fillerWords = listOf(
                "spent", "spend", "paid", "pay", "bought", "buy", "debited", "received", "receive", "got",
                "credited", "earned", "mila", "mile", "on", "for", "at", "to", "from", "in", "of", "a", "an",
                "the", "my", "me", "i", "and", "ka", "ki", "ke", "ko", "se", "mein", "liye", "diya",
                "diye", "kiya", "kiye", "kharcha", "kharch", "raat", "subah", "shaam", "around", "about",
                "approx", "just", "only", "total", "amount", "worth"
            ),
            noteWords = listOf("for", "with"),
            categoryKeywords = listOf(
                "swiggy" to CategoryHint.FOOD, "zomato" to CategoryHint.FOOD,
                "restaurant" to CategoryHint.FOOD, "cafe" to CategoryHint.FOOD,
                "coffee" to CategoryHint.FOOD, "chai" to CategoryHint.FOOD, "tea" to CategoryHint.FOOD,
                "lunch" to CategoryHint.FOOD, "dinner" to CategoryHint.FOOD,
                "breakfast" to CategoryHint.FOOD, "khana" to CategoryHint.FOOD,
                "pizza" to CategoryHint.FOOD, "burger" to CategoryHint.FOOD,
                "dominos" to CategoryHint.FOOD, "kfc" to CategoryHint.FOOD,
                "mcdonalds" to CategoryHint.FOOD, "starbucks" to CategoryHint.FOOD,
                "bakery" to CategoryHint.FOOD, "dhaba" to CategoryHint.FOOD, "food" to CategoryHint.FOOD,
                "dmart" to CategoryHint.GROCERIES, "blinkit" to CategoryHint.GROCERIES,
                "zepto" to CategoryHint.GROCERIES, "bigbasket" to CategoryHint.GROCERIES,
                "instamart" to CategoryHint.GROCERIES, "grocery" to CategoryHint.GROCERIES,
                "groceries" to CategoryHint.GROCERIES, "kirana" to CategoryHint.GROCERIES,
                "sabzi" to CategoryHint.GROCERIES, "vegetables" to CategoryHint.GROCERIES,
                "milk" to CategoryHint.GROCERIES, "doodh" to CategoryHint.GROCERIES,
                "supermarket" to CategoryHint.GROCERIES, "mart" to CategoryHint.GROCERIES,
                "petrol" to CategoryHint.FUEL, "diesel" to CategoryHint.FUEL, "fuel" to CategoryHint.FUEL,
                "cng" to CategoryHint.FUEL, "indian oil" to CategoryHint.FUEL,
                "uber" to CategoryHint.TRANSPORT, "ola" to CategoryHint.TRANSPORT,
                "rapido" to CategoryHint.TRANSPORT, "metro" to CategoryHint.TRANSPORT,
                "auto" to CategoryHint.TRANSPORT, "cab" to CategoryHint.TRANSPORT,
                "taxi" to CategoryHint.TRANSPORT, "bus" to CategoryHint.TRANSPORT,
                "parking" to CategoryHint.TRANSPORT, "toll" to CategoryHint.TRANSPORT,
                "amazon" to CategoryHint.SHOPPING, "flipkart" to CategoryHint.SHOPPING,
                "myntra" to CategoryHint.SHOPPING, "ajio" to CategoryHint.SHOPPING,
                "meesho" to CategoryHint.SHOPPING, "shopping" to CategoryHint.SHOPPING,
                "clothes" to CategoryHint.SHOPPING, "shoes" to CategoryHint.SHOPPING,
                "electricity" to CategoryHint.BILLS, "bijli" to CategoryHint.BILLS,
                "wifi" to CategoryHint.BILLS, "broadband" to CategoryHint.BILLS,
                "recharge" to CategoryHint.BILLS, "water bill" to CategoryHint.BILLS,
                "gas" to CategoryHint.BILLS, "bill" to CategoryHint.BILLS,
                "rent" to CategoryHint.RENT, "kiraya" to CategoryHint.RENT,
                "pharmacy" to CategoryHint.HEALTH, "medicine" to CategoryHint.HEALTH,
                "medicines" to CategoryHint.HEALTH, "dawai" to CategoryHint.HEALTH,
                "doctor" to CategoryHint.HEALTH, "hospital" to CategoryHint.HEALTH,
                "apollo" to CategoryHint.HEALTH, "gym" to CategoryHint.HEALTH,
                "movie" to CategoryHint.ENTERTAINMENT, "movies" to CategoryHint.ENTERTAINMENT,
                "cinema" to CategoryHint.ENTERTAINMENT, "pvr" to CategoryHint.ENTERTAINMENT,
                "inox" to CategoryHint.ENTERTAINMENT, "bookmyshow" to CategoryHint.ENTERTAINMENT,
                "netflix" to CategoryHint.SUBSCRIPTIONS, "spotify" to CategoryHint.SUBSCRIPTIONS,
                "hotstar" to CategoryHint.SUBSCRIPTIONS, "prime" to CategoryHint.SUBSCRIPTIONS,
                "youtube" to CategoryHint.SUBSCRIPTIONS, "subscription" to CategoryHint.SUBSCRIPTIONS,
                "flight" to CategoryHint.TRAVEL, "irctc" to CategoryHint.TRAVEL,
                "train" to CategoryHint.TRAVEL, "hotel" to CategoryHint.TRAVEL,
                "makemytrip" to CategoryHint.TRAVEL, "trip" to CategoryHint.TRAVEL,
                "fees" to CategoryHint.EDUCATION, "fee" to CategoryHint.EDUCATION,
                "tuition" to CategoryHint.EDUCATION, "course" to CategoryHint.EDUCATION,
                "books" to CategoryHint.EDUCATION, "school" to CategoryHint.EDUCATION,
                "salon" to CategoryHint.PERSONAL_CARE, "haircut" to CategoryHint.PERSONAL_CARE,
                "parlour" to CategoryHint.PERSONAL_CARE, "spa" to CategoryHint.PERSONAL_CARE,
                "emi" to CategoryHint.EMI, "loan" to CategoryHint.EMI,
                "salary" to CategoryHint.SALARY, "tankhwah" to CategoryHint.SALARY,
                "stipend" to CategoryHint.SALARY, "bonus" to CategoryHint.SALARY,
                "freelance" to CategoryHint.FREELANCE, "client" to CategoryHint.FREELANCE,
                "interest" to CategoryHint.INTEREST, "fd" to CategoryHint.INTEREST,
                "refund" to CategoryHint.REFUND, "refunded" to CategoryHint.REFUND,
                "cashback" to CategoryHint.REFUND,
                "gift" to CategoryHint.GIFT_RECEIVED, "shagun" to CategoryHint.GIFT_RECEIVED
            ),
            genericWords = listOf(
                "food", "lunch", "dinner", "breakfast", "khana", "grocery", "groceries", "shopping",
                "fuel", "bill", "fees", "fee", "subscription", "trip"
            ),
            shortKeywordLength = 4,
            titleMaxLength = 40,
            splitWords = listOf("and", "aur")
        )
    }
}
