package com.demo.creditlimit.network.model.request2

data class KycConfigResp(
    val addressProperty: List<ConfigResp>,
    val designation: List<ConfigResp>,
    val education: List<ConfigResp>,
    val expectedLoanAmount: List<ConfigResp>,
    val industry: List<ConfigResp>,
    val loanPurpose: List<ConfigResp>,
    val maritalStatus: List<ConfigResp>,
    val monthlyIncome: List<ConfigResp>,
    val numberOfChildren: List<ConfigResp>,
    val payMethod: List<ConfigResp>,
    val relation: List<ConfigResp>,
    val religion: List<ConfigResp>,
    val workType: List<ConfigResp>,
    val workingTime: List<ConfigResp>,
)