package com.example

import com.example.data.entity.ShoppingItemEntity
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun shoppingItem_toggleChecked_worksCorrectly() {
    val item = ShoppingItemEntity(
      id = 1L,
      planId = 10L,
      name = "Petto di pollo",
      quantity = "300g",
      category = "Carne, Pesce & Uova",
      isChecked = false
    )
    val toggled = item.copy(isChecked = !item.isChecked)
    assertTrue(toggled.isChecked)
    assertEquals("Petto di pollo", toggled.name)
    assertEquals("Carne, Pesce & Uova", toggled.category)
  }

  @Test
  fun shoppingItem_customItem_propertiesPreserved() {
    val custom = ShoppingItemEntity(
      id = 2L,
      planId = 15L,
      name = "Olio EVO",
      quantity = "1L",
      category = "Condimenti & Dispensa",
      isChecked = true,
      isCustom = true
    )
    assertTrue(custom.isCustom)
    assertTrue(custom.isChecked)
    assertEquals(15L, custom.planId)
  }
}
