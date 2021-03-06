package com.wintv.lottery.bet.action;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal; 

import javax.servlet.ServletContext;

import net.sf.json.JSONObject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.xwork.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.util.ServletContextAware;
import org.springframework.beans.factory.annotation.Autowired;

import com.wintv.framework.acegi.UserCookie;
import com.wintv.framework.common.BaseAction;
import com.wintv.framework.common.Constants;
import com.wintv.framework.exception.LotteryBizException;
import com.wintv.lottery.admin.phase.vo.LotteryPhasePO;
import com.wintv.lottery.bet.service.BetService;
import com.wintv.lottery.bet.utils.CommUtil;
import com.wintv.framework.utils.DateUtil;

/**
 * 发起合买模块公用Action 
 * 
 * 涉及到彩种有:胜负14场,任选9场,6场半全场,4场进球 (注:不包括北京单场)
 * 
 * @author Hikin Yao
 *
 * @version 1.0.0
 */
@SuppressWarnings("unchecked")
public class ReleaseTogetherBuyAction extends BaseAction implements ServletContextAware{
	private static final long serialVersionUID = 6973283936727479171L;
	private ServletContext context;
	private String type;// 彩种类别   6:胜负彩,   7:任九,  8:6场半全场,  9:4场进球
	private Long phaseId;//期次ID
	private String fqType;// '1'单式发起合买    '2' 复式发起合买
	private String uploadType;// '1'立即上传/选择号 '2'稍后上传/选择号
	@Autowired
	private BetService betService;
	//合买信息的一些基本参数
	private String betPlan;// 投注内容
	private String endTime;// 方案截止时间
	private String categoryType;// 1:足球单场,6:胜负彩,7:任九,8:6场半全场,9:4场进球) |购买b2c(10:) |购买c2c(11) 12:充值 13:取款 14:缴纳保证金15:心水保证金解冻
	private String betCategory;// 投注彩种 1: 胜负14场 2:任9场 3:4场进球 5:6 场半全场 61:单场半全场 62:单场比分 63:单场胜平负 64:单场上下单双 65:单场总进球
	private String phaseNo;// 期次号
	private String betMulti;// 投注倍数
	private String isUseCaijin;// 是否使用彩金
	private String betNum;//总投注数
	private String divideCopys;//分成份数
	private String floorCopys;//保底份数
	private String subscribeCopys;//认购份数
	private String tcRate;//方案提成
	private String totalBetMoney;//总金额
	private String myBuyTotalMoney;//我的总认购金额
	private String eachFenshuMoney;//每份金额
	
	private String planTitle;//方案标题
	private String planDesc;//方案描述
	private String secretType;//保密类型
	private String betSelectedInfo;//投注信息
	private String recuitType;//招股类型
	private String recuitUsers;//招股彩友用户名
	private static String[] BET_CATEGORY_ARRAY={"1","2","5","3"};
	
	// 封装上传文件的属性
	private File doc;
	// 封装上传文件名称属性
	private String fileName;
	// 封装上传文件类型属性
	private String contentType;
	// 保存文件路径属性
	private String dir;
	// 保存文件名称属性
	private String targetFileName;
	//上传文件临时文件
	private static String TEMP_UPLOAD_FILE="TEMP_UPLOAD_FILE";


	public void setDoc(File doc) {
		this.doc = doc;
	}

	public void setDocFileName(String fileName) {
		this.fileName = fileName;
	}

	public void setDocContentType(String contentType) {
		this.contentType = contentType;
	}
	
	public String excute() {
		return SUCCESS;
	}
	/**
	 * 获取最近几期的期次列表
	 */
	public String getLatestPhaseList() {
		try {
			List result = null;
			Map<String, Object> params = new HashMap<String, Object>();
			//'6':胜负彩期次    '9':进球彩期次 '8':半全场期次 '1':北京单场期次
			params.put("phaseCategory", this.getCategoryType());
			result = betService.findLatestPhaseList(params);
			if (isNotNull(result)) {
				generateResult(1, MSG_SUCCESS, result);
			} else {
				generateResult(0, MSG_FAILURE, "errors");
			}
		} catch (Exception e) {
			generateResult(0, MSG_FAILURE, "errors");
			e.printStackTrace();
			throw new LotteryBizException(e.getLocalizedMessage());
		}
		return SUCCESS;
	}
	/**
	 * 获取对阵比赛赛事列表
	 * 
	 */
	public String getPhaseAgainstRacesList() {
		try {
			Map result = null;
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("phaseCategory", this.getType());
			params.put("betCategory", BET_CATEGORY_ARRAY[Integer.parseInt(this.getType())-6]);
			params.put("phaseId", this.getPhaseId());
			params.put("isCurrent", "0");
			params.put("type", "4");
			result = betService.findPhaseAgainstList(params);
			if (isNotNull(result)) {
				LotteryPhasePO po=(LotteryPhasePO)result.get("po");
				result.put("mulRemainTime", remainSeconds(po.getMulDeadline()));
				result.put("singleRemainTime", remainSeconds(po.getSingleDeadline()));
				generateResult(1, MSG_SUCCESS, result);
			} else {
				generateResult(0, MSG_FAILURE, "errors");
			}
		} catch (Exception e) {
			generateResult(0, MSG_FAILURE, "errors");
			e.printStackTrace();
			throw new LotteryBizException(e.getLocalizedMessage());
		}
		return SUCCESS;
	}
	//计算到达截至时间的剩余时间
	private Long remainSeconds(String deadline){
		if(isNotNull(deadline)&&""!=deadline){
			Date deadlineDate=DateUtil.parseDate(deadline,"yyyy-MM-dd HH:mm");
			//Date systemDate=DateUtil.add(new Date(),-2);
			Date systemDate=new Date();
			Long remainTime=deadlineDate.getTime()-systemDate.getTime();
			remainTime=remainTime>0L?remainTime:0L;
			remainTime=remainTime/1000;
			return remainTime;
		}else{
			return 0L;
		}
	}
	/**
	 * 发起合买-立即投注
	 * 
	 * @return  1:投注成功 -1:系统报错,不成功 4:余额不足 ，但仍然能投注 5:账户锁定
	 */
	public String saveBetOrder() {
		try {
			Map params = generateFshmParamsMap();
			//判断用户是否已登录
			Boolean isLogin=(Boolean)params.get("isLogin");
			if(isLogin){//用户登录了执行保存操作
				//单式发起立即上传时,将上传文件路径放到参数map中去
				params=putTxtPathToParamsMap(params);
				long result = betService.saveBetCoopOrder(params);
				if (isNotNull(result)) {
					generateResult(1, MSG_SUCCESS, result);
				} else {
					generateResult(0, MSG_FAILURE, "errors");
				}
			}else{
				generateResult(0, MSG_FAILURE, "errors");
			}
		} catch (Exception e) {
			generateResult(0, MSG_FAILURE, "errors");
			e.printStackTrace();
			throw new LotteryBizException(e.getLocalizedMessage());
		}
		return SUCCESS;
	}
	/**
	 * 各彩种的上传文件格式验证。
	 * 
	 * @return String
	 */
	public void validator() {
		try {
			if(session.containsKey(TEMP_UPLOAD_FILE)){
				session.remove(TEMP_UPLOAD_FILE);
			}
			if(isNotNull(doc)&&StringUtils.isNotBlank(fileName)){
				result=CommUtil.doValidator(doc,fileName,this.getType());
				System.out.println("----"+doc.getAbsolutePath()+"-----fileName="+fileName+"---type="+this.getType());
				if(result.getMessage().equals("success")){
					Map oneFile=new HashMap();
					//将文档写到内存进行缓存
					oneFile.put("doc", FileUtils.readFileToString(doc));
					oneFile.put("fileName",fileName);
					session.put(TEMP_UPLOAD_FILE, oneFile);
				}
			}else{
				generateResult(0, MSG_FAILURE, "上传文件出错：上传文件不存在,请检查文件路径名是否正确！");
			}
		} catch (Exception e) {
			generateResult(0, MSG_FAILURE, "errors");
			e.printStackTrace();
			throw new LotteryBizException(e.getLocalizedMessage());
		}
		JSONObject obj = new JSONObject();
		obj.put("result", result);
		ajaxForAction(obj.toString());//返回验证结果
	}
	/**
	 * 单式发起立即上传时,将上传文件路径放到参数map中去
	 * @param params
	 * @return
	 */
	private Map putTxtPathToParamsMap(Map params){
		Map result=params;
		String fqType=this.getFqType();//单式发起
		String uploadType=this.getUploadType();//立即上传
		System.out.println("-----fqType="+fqType+"-------uploadType="+uploadType+"-------");
		if(null!=fqType&&fqType.trim().equals("2")){//单式发起
			if(null!=uploadType&&uploadType.trim().equals("1")){//立即上传
				String txtPath=genUploadFilePath();
				System.out.println("-----txtPath="+txtPath+"-------");
				result.put("txtPath", txtPath);

			}
		}
		return result;
	}
	/**
	 * 上传文件到服务器同时返回生成上传文件的路径名称
	 */
	private String genUploadFilePath(){
		String txtPath=null;
		if(session.containsKey(TEMP_UPLOAD_FILE)){
			Map oneFile=(Map)session.get(TEMP_UPLOAD_FILE);
			String data=(String)oneFile.get("doc");
			String fileName=(String)oneFile.get("fileName");
			try {
			// 获得upload路径的实际目录
			String realPath = ServletActionContext
					.getServletContext().getInitParameter(
							"uploadPath");
			String targetDirectory = realPath+ "\\" + Constants.UPLOAD_PALN_FILE;
			// 生成保存文件的文件名称
			targetFileName = CommUtil.generateFileName(fileName);
			Integer categoryType=Integer.parseInt(this.getCategoryType());
			// 保存文件的路径
			setDir(targetDirectory + "\\" + BET_CATEGORY_ARRAY[categoryType-6] + "\\" + phaseId + "\\" + targetFileName);
			// 建立一个目标文件
			File target = new File(targetDirectory + "\\" + BET_CATEGORY_ARRAY[categoryType-6] + "\\" + phaseId, targetFileName);
			System.out.println(target.getAbsolutePath());
			// 将临时文件复制到目标文件
			//FileUtils.w.copyFile(doc, target);
			// 将临时文件将临时文件从内存中读出写到目标文件
			FileUtils.writeStringToFile(target, data);
			/* 用户上传式投注 */
			txtPath = targetFileName;
			// 设定文件保存路径
			} catch (Exception e) {
				e.printStackTrace();
				throw new LotteryBizException(e.getLocalizedMessage());
			}
		}
		return txtPath;
	}
	/**
	 * 后台参数
	 */
	private Map<String, Object> generateFshmParamsMap() {
		UserCookie currentUser = (UserCookie) session.get("userCookie");
		Map<String, Object> queryParams = new HashMap<String, Object>();
		Integer categoryType=Integer.parseInt(this.getCategoryType());
		//复式发起立即选择号时 保存选择号的内容
		if(isNotNull(this.getUploadType(),this.getFqType())&&this.getUploadType().equals("1")&&this.getFqType().equals("4")){
			if(categoryType==7){//任选九场 betContent 立即选择号
				queryParams.put("betContent", this.getBetPlan());
			}else{
				queryParams.put("plan", this.getBetPlan());
			}
		}
		queryParams.put("endTime", this.getEndTime());
		queryParams.put("categoryType", this.getCategoryType());
		queryParams.put("betCategory", BET_CATEGORY_ARRAY[categoryType-6]);
		queryParams.put("phaseNo", this.getPhaseNo());
		queryParams.put("phaseId", this.getPhaseId());
		
		queryParams.put("betMulti", this.getBetMulti());
		queryParams.put("useCaijin", this.getIsUseCaijin());
		queryParams.put("type", this.getFqType());
		
		//总投注数
		queryParams.put("betNum", this.getBetNum());
		queryParams.put("divideCopys", this.getDivideCopys());
		queryParams.put("floorCopys", this.getFloorCopys());
		queryParams.put("subscribeCopys", this.getSubscribeCopys());
		queryParams.put("tcRate", this.getTcRate());

		processCalculateMoney(queryParams);
		/*
		queryParams.put("allMoney", this.getTotalBetMoney());
		queryParams.put("singleMoney", this.getEachFenshuMoney());
		queryParams.put("subscribeMoney", this.getMyBuyTotalMoney());
		*/
		queryParams.put("planTitle", this.getPlanTitle());
		queryParams.put("planDesc", this.getPlanDesc());
		
		queryParams.put("securityType", this.getSecretType());
		
		if(isNotNull(this.getFqType())&&this.getFqType().equals("4")){//复式选择号时才有选择号信息
			queryParams.put("betSelInfo",this.getBetSelectedInfo());
		}
		/* 目前不需要此功能
		if(isNotNull(this.getRecuitType())&&this.getRecuitType().equals("2")){//招股对象向固定彩友招股时
			queryParams.put("recuitUsers",this.getRecuitUsers());
		}*/
		if (isNotNull(currentUser)) {
			queryParams.put("betUserid", currentUser.getUserId());
			queryParams.put("betUsername", currentUser.getUsername());
			queryParams.put("isLogin", true);
		}else{
			queryParams.put("isLogin", false);
		}
		return queryParams;
	}
	
	private void processCalculateMoney(Map queryParams){
		BigDecimal allMoney=new BigDecimal(0);//总投注金额
		BigDecimal singleMoney=new BigDecimal(0);//每份金额
		BigDecimal subscribeMoney=new BigDecimal(0);//认购金额
		if (isNotNull(queryParams)) {
			Integer betNum=Integer.parseInt((String)queryParams.get("betNum"));//总投注数
			Integer divideCopys=Integer.parseInt((String)queryParams.get("divideCopys"));//分成份数
			Integer subscribeCopys=Integer.parseInt((String)queryParams.get("subscribeCopys"));//认购份数
			Integer betMulti=Integer.parseInt((String)queryParams.get("betMulti"));//投注倍数
			if(isNotNull(betNum,divideCopys,subscribeCopys,betMulti)){
				allMoney=new BigDecimal(betNum*betMulti*2);//总投注金额
				singleMoney=new BigDecimal(0);//每份金额
				if(!divideCopys.equals(0)){//分成份数不为0时
					singleMoney=allMoney.divide(new BigDecimal(divideCopys));
				}
				subscribeMoney=singleMoney.multiply(new BigDecimal(subscribeCopys));//认购金额
			}
		}
		//设置金额精度
		allMoney=allMoney.setScale(2, BigDecimal.ROUND_HALF_UP);
		singleMoney=singleMoney.setScale(2, BigDecimal.ROUND_HALF_UP);
		subscribeMoney=subscribeMoney.setScale(2, BigDecimal.ROUND_HALF_UP);
		//将金额放进map中去
		queryParams.put("allMoney", allMoney);
		queryParams.put("singleMoney", singleMoney.toString());
		queryParams.put("subscribeMoney", subscribeMoney.toString());
	}
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Long getPhaseId() {
		return phaseId;
	}
	public void setPhaseId(Long phaseId) {
		this.phaseId = phaseId;
	}
	public String getFqType() {
		return fqType;
	}
	public void setFqType(String fqType) {
		this.fqType = fqType;
	}
	public BetService getBetService() {
		return betService;
	}
	public void setBetService(BetService betService) {
		this.betService = betService;
	}
	
	public String getBetPlan() {
		return betPlan;
	}
	public void setBetPlan(String betPlan) {
		this.betPlan = betPlan;
	}
	public String getEndTime() {
		return endTime;
	}
	public void setEndTime(String endTime) {
		this.endTime = endTime;
	}
	public String getCategoryType() {
		return categoryType;
	}
	public void setCategoryType(String categoryType) {
		this.categoryType = categoryType;
	}
	public String getBetCategory() {
		return betCategory;
	}
	public void setBetCategory(String betCategory) {
		this.betCategory = betCategory;
	}
	public String getPhaseNo() {
		return phaseNo;
	}
	public void setPhaseNo(String phaseNo) {
		this.phaseNo = phaseNo;
	}

	public String getBetMulti() {
		return betMulti;
	}
	public void setBetMulti(String betMulti) {
		this.betMulti = betMulti;
	}
	public String getIsUseCaijin() {
		return isUseCaijin;
	}
	public void setIsUseCaijin(String isUseCaijin) {
		this.isUseCaijin = isUseCaijin;
	}
	public String getBetNum() {
		return betNum;
	}
	public void setBetNum(String betNum) {
		this.betNum = betNum;
	}
	public String getDivideCopys() {
		return divideCopys;
	}
	public void setDivideCopys(String divideCopys) {
		this.divideCopys = divideCopys;
	}
	public String getFloorCopys() {
		return floorCopys;
	}
	public void setFloorCopys(String floorCopys) {
		this.floorCopys = floorCopys;
	}
	public String getSubscribeCopys() {
		return subscribeCopys;
	}
	public void setSubscribeCopys(String subscribeCopys) {
		this.subscribeCopys = subscribeCopys;
	}
	public String getTcRate() {
		return tcRate;
	}
	public void setTcRate(String tcRate) {
		this.tcRate = tcRate;
	}
	public String getTotalBetMoney() {
		return totalBetMoney;
	}
	public void setTotalBetMoney(String totalBetMoney) {
		this.totalBetMoney = totalBetMoney;
	}
	public String getMyBuyTotalMoney() {
		return myBuyTotalMoney;
	}
	public void setMyBuyTotalMoney(String myBuyTotalMoney) {
		this.myBuyTotalMoney = myBuyTotalMoney;
	}
	public String getEachFenshuMoney() {
		return eachFenshuMoney;
	}
	public void setEachFenshuMoney(String eachFenshuMoney) {
		this.eachFenshuMoney = eachFenshuMoney;
	}
	public String getPlanTitle() {
		return planTitle;
	}
	public void setPlanTitle(String planTitle) {
		this.planTitle = planTitle;
	}
	public String getPlanDesc() {
		return planDesc;
	}
	public void setPlanDesc(String planDesc) {
		this.planDesc = planDesc;
	}
	public void setServletContext(ServletContext context) {
        this.context = context;
    }

	public ServletContext getContext() {
		return context;
	}

	public void setContext(ServletContext context) {
		this.context = context;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getDir() {
		return dir;
	}

	public void setDir(String dir) {
		this.dir = dir;
	}

	public String getTargetFileName() {
		return targetFileName;
	}

	public void setTargetFileName(String targetFileName) {
		this.targetFileName = targetFileName;
	}

	public File getDoc() {
		return doc;
	}

	public String getUploadType() {
		return uploadType;
	}

	public void setUploadType(String uploadType) {
		this.uploadType = uploadType;
	}

	public String getSecretType() {
		return secretType;
	}

	public void setSecretType(String secretType) {
		this.secretType = secretType;
	}

	public String getBetSelectedInfo() {
		return betSelectedInfo;
	}

	public void setBetSelectedInfo(String betSelectedInfo) {
		this.betSelectedInfo = betSelectedInfo;
	}

	public String getRecuitType() {
		return recuitType;
	}

	public void setRecuitType(String recuitType) {
		this.recuitType = recuitType;
	}

	public String getRecuitUsers() {
		return recuitUsers;
	}

	public void setRecuitUsers(String recuitUsers) {
		this.recuitUsers = recuitUsers;
	}
	
	
}
