package com.akshu.cocwidget

// Extend freely: id -> display name. Unknown ids show as "ID <number>".
object Names {
    val map = mapOf(
        1000001 to "Town Hall", 1000002 to "Elixir Collector", 1000004 to "Gold Mine",
        1000013 to "Mortar", 1000097 to "Crafting Station",
        28000000 to "Barbarian King", 28000001 to "Archer Queen", 28000002 to "Grand Warden",
        28000004 to "Royal Champion", 28000006 to "Minion Prince",
        4000000 to "Barbarian", 4000001 to "Archer", 4000002 to "Goblin", 4000003 to "Giant",
        4000004 to "Wall Breaker", 4000005 to "Balloon", 4000006 to "Wizard", 4000007 to "Healer",
        4000008 to "Dragon", 4000009 to "P.E.K.K.A", 4000010 to "Baby Dragon", 4000011 to "Miner",
        4000012 to "Electro Dragon", 4000013 to "Yeti",
        73000000 to "L.A.S.S.I", 73000001 to "Electro Owl", 73000002 to "Mighty Yak",
        73000003 to "Unicorn", 73000004 to "Frosty", 73000007 to "Diggy",
        73000008 to "Poison Lizard", 73000009 to "Phoenix", 73000010 to "Spirit Fox", 73000011 to "Angry Jelly",
        93000000 to "Apprentice", 93000001 to "Assistant", 93000002 to "Alchemist"
    )
    fun of(id: Int) = map[id] ?: "ID $id"
}
