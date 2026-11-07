package com.ledgeryaar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledgeryaar.domain.model.Asset
import com.ledgeryaar.domain.model.BankAccount
import com.ledgeryaar.domain.model.Goal
import com.ledgeryaar.domain.model.Installment
import com.ledgeryaar.domain.model.Loan
import com.ledgeryaar.domain.model.Person
import com.ledgeryaar.domain.model.Transaction
import com.ledgeryaar.domain.model.TxType
import com.ledgeryaar.domain.usecase.LedgerActions
import com.ledgeryaar.domain.usecase.LedgerData
import com.ledgeryaar.domain.usecase.ObserveLedgerDataUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ViewModel مشترک همهٔ صفحه‌ها: داده‌ی کل دفتر + همهٔ عملیات افزودن/ویرایش/حذف */
class LedgerViewModel(
    observeLedgerData: ObserveLedgerDataUseCase,
    private val actions: LedgerActions
) : ViewModel() {

    val state: StateFlow<LedgerData> = observeLedgerData()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LedgerData())

    fun saveTransaction(tx: Transaction) { viewModelScope.launch { actions.saveTransaction(tx) } }
    fun deleteTransaction(id: Long) { viewModelScope.launch { actions.deleteTransaction(id) } }

    fun saveLoan(loan: Loan) { viewModelScope.launch { actions.saveLoan(loan) } }
    fun deleteLoan(id: Long) { viewModelScope.launch { actions.deleteLoan(id) } }
    fun toggleInstallment(item: Installment) { viewModelScope.launch { actions.toggleInstallment(item) } }

    fun savePerson(person: Person) { viewModelScope.launch { actions.savePerson(person) } }
    fun togglePersonPaid(person: Person) { viewModelScope.launch { actions.togglePersonPaid(person) } }
    fun deletePerson(id: Long) { viewModelScope.launch { actions.deletePerson(id) } }

    fun saveBudget(category: String, amount: Double) { viewModelScope.launch { actions.saveBudget(category, amount) } }
    fun applyBudgets(limits: List<Pair<String, Double>>) { viewModelScope.launch { actions.applyBudgets(limits) } }
    fun deleteBudget(id: Long) { viewModelScope.launch { actions.deleteBudget(id) } }

    fun saveGoal(goal: Goal) { viewModelScope.launch { actions.saveGoal(goal) } }
    fun deleteGoal(id: Long) { viewModelScope.launch { actions.deleteGoal(id) } }

    fun saveBank(bank: BankAccount) { viewModelScope.launch { actions.saveBank(bank) } }
    fun deleteBank(id: Long) { viewModelScope.launch { actions.deleteBank(id) } }
    fun saveAsset(asset: Asset) { viewModelScope.launch { actions.saveAsset(asset) } }
    fun deleteAsset(id: Long) { viewModelScope.launch { actions.deleteAsset(id) } }

    fun addCategory(name: String, type: TxType) { viewModelScope.launch { actions.addCategory(name, type) } }
}
