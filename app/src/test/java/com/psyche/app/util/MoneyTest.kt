package com.psyche.app.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MoneyTest {

    @Test
    fun `everyday entries stay under one million pesos`() {
        assertThat(Money.parse("999999.99")).isEqualTo(99_999_999)
        assertThat(Money.parse("1000000")).isNull()
        assertThat(Money.parse("0")).isNull()
    }

    @Test
    fun `debt principals can exceed one million pesos`() {
        assertThat(Money.parseDebt("2000000")).isEqualTo(200_000_000)
        assertThat(Money.parseDebt("2000000.50")).isEqualTo(200_000_050)
        assertThat(Money.parseDebt("1000000000")).isNull()
        assertThat(Money.parseDebt("0")).isNull()
    }

    @Test
    fun `large debt amounts still format`() {
        assertThat(Money.formatPeso(200_000_000)).isEqualTo("\u20B12,000,000")
    }
}
