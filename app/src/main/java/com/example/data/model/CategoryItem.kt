package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorHex: String,
    val iconName: String = "tag",
    val isDefault: Boolean = false
) {
    companion object {
        fun defaultCategories(): List<CategoryItem> = listOf(
            CategoryItem(id = 1, name = "Work", colorHex = "#6366F1", iconName = "work", isDefault = true),
            CategoryItem(id = 2, name = "Study", colorHex = "#06B6D4", iconName = "school", isDefault = true),
            CategoryItem(id = 3, name = "Health", colorHex = "#10B981", iconName = "fitness", isDefault = true),
            CategoryItem(id = 4, name = "Personal", colorHex = "#F59E0B", iconName = "person", isDefault = true),
            CategoryItem(id = 5, name = "Chores", colorHex = "#F97316", iconName = "home", isDefault = true)
        )
    }
}
