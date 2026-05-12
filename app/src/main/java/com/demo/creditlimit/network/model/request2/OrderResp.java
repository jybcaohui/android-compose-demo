package com.demo.creditlimit.network.model.request2;

import com.demo.creditlimit.network.model.request2.RepaymentPlan;

import java.util.Map;
import java.util.List;

// OrderResp -> hagxwh
public class OrderResp {
    public String id; // id -> cgkz 
    public int productId; // productId -> afurgkslg 
    public String productName; // productName -> tsdjxwkar 
    public String img; // img -> ijru 
    public int loanTerm; // loanTerm -> vnzuljqh 
    public int loanTermUnit; // loanTermUnit -> aemmytuyp 
    public double loanAmount; // loanAmount -> ajks 
    public double interestMax; // interestMax -> qiofxo 
    public double interestMin; // interestMin -> kchxofpxwy 
    public double currentRepaymentAmount; // currentRepaymentAmount -> gje 
    public double actualAmount; // actualAmount -> ikc 
    public String bankCard; // bankCard -> ijod 
    public String bankName; // bankName -> nqoscne 
    public String ifscCode; // ifscCode -> mwwhebxcf 
    public int bankId; // bankId -> tmdbd 
    public boolean cooperation; // cooperation -> iyh 
    public int fillStatus; // fillStatus -> gpvt 
    public int fillBankCardId; // fillBankCardId -> joaflutebo 
    public int allowDelay; // allowDelay -> vnysmkxopgpl 
    public int status; // status -> hgnzqrx 
    public boolean api; // api -> fsywem 
    public boolean checkInfo; // checkInfo -> nedtavpy 
    public String createTime; // createTime -> fizuvcn 
    public int delayNum; // delayNum -> natp 
    public int delayTotalNum; // delayTotalNum -> nlyy 
    public String repaymentTime; // repaymentTime -> pewk 
    public String repaymentedTime; // repaymentedTime -> mdz 
    public int overdueDays; // overdueDays -> kfrco 
    public boolean canApply; // canApply -> lbkwylv 
    public boolean canApplyOC; // canApplyOC -> hmgvmmvo 
    public boolean canUseCoupon; // canUseCoupon -> cavgmkqoy 
    public List<Integer> usedCouponList; // usedCouponList -> nlpjgsywlnso 
    public int auditStatus; // auditStatus -> rujbtsrn // 1-今日额度已上限，2-重新申请。3-重新机审 审批状态，机审失败的订单会返回该字段
    public double realAmount; // realAmount -> fhgcdogsa // 当前可借额度
    public List<RepaymentPlan> repaymentPlanList; // repaymentPlanList -> mod // 还款计划
    public int entrance; // entrance -> pbmvfmt 
    public String identCode; // identCode -> bzu // 识别码
    public String serviceDate; // serviceDate -> krenvfkqw // 服务日期
    public int isReloan; // isReloan -> kks // 是否复借
    public double walletDeduct; // walletDeduct -> zmjknla 
    public double cashbackDeduct; // cashbackDeduct -> etz 
    public double amountMin; // amountMin -> tatdrnudc // 最低借款金额
    public double remainAmount; // remainAmount -> ovttu // 剩余额度
    public int userType; // userType -> eqak // 用户类型
    public double cashbackRate; // cashbackRate -> dklvkkb // 返利利率
    public boolean isWalletLoan; // isWalletLoan -> gkfcv // 是否放款到钱包
    public double balance; // balance -> wrjvroxl // 钱包余额
    public double hdsakplzhae; // hdsakplzhae ->  
    public boolean jijajvtvi; // jijajvtvi ->  
    public double wzbh; // wzbh ->  
    public String pfchkadbzzln; // pfchkadbzzln ->  
    public int obayz; // obayz ->  
    public double ykmp; // ykmp ->  
}
