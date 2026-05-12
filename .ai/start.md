以当前目录 android-compose-demo 作为项目根目录，创建一个 Android 项目。

技术要求：
- 使用 Kotlin + Jetpack Compose
- 使用 Material3
- 最低支持 Android 8.0（API 26）
- 使用 Gradle Kotlin DSL（build.gradle.kts）
- 使用 MVVM 架构
- 使用 Navigation Compose 实现页面导航
- 使用 ViewModel 管理页面状态
- 使用 StateFlow + UiState
- 目录结构清晰，符合生产级项目规范
- 仅支持英文，不需要国际化/多语言
- 不需要接入真实接口，先使用 mock 数据
- 所有页面先完成基础框架和导航，不需要复杂 UI
- 代码要求可直接运行
- 不要生成 XML 布局，全部使用 Compose
- 所有页面使用独立 composable screen
- 页面样式简洁，接近金融类 App 风格

项目功能：
这是一个“Credit Limit Estimation（信用额度试算）”应用。

需要包含以下页面：

1. Splash Page
- App Logo
- 跳转首页

2. Home Page
- 产品介绍
- “Start Evaluation”按钮
- 跳转登录页

3. Login Page
- Phone Number 输入框
- OTP 输入框
- Login 按钮
- 登录成功后进入 KYC 流程

4. KYC 流程
   使用分步流程（step flow）：

4.1 Basic Information Page
字段：
- Full Name
- Gender
- Date of Birth
- ID Number
- Address

4.2 Emergency Contact Page
字段：
- Contact Name
- Relationship
- Phone Number

4.3 Additional Information Page
字段：
- Occupation
- Monthly Income
- Education Level

每一步：
- 包含顶部 TopAppBar
- 包含 Back 按钮
- 包含 Continue 按钮
- 使用表单状态管理
- 先不做真实校验

5. Credit Result Page
   展示：
- Estimated Credit Limit
- Loan Period
- Interest Rate
- Apply Now 按钮
- Back Home 按钮

项目结构要求：

app/src/main/java/.../

包含：
- ui/
- ui/screens/
- ui/components/
- navigation/
- viewmodel/
- model/
- repository/
- theme/

需要实现：
- AppNavHost
- Screen Route 管理
- Base UI State
- Fake Repository
- 页面间参数传递
- Compose Preview

依赖要求：
- androidx.navigation:navigation-compose
- lifecycle-viewmodel-compose
- material3
- kotlinx-coroutines

UI要求：
- 使用 MaterialTheme.colorScheme
- 使用统一间距
- 使用 Column + LazyColumn
- 表单页支持滚动
- 顶部栏统一风格
- 不要使用实验性 API（避免 ExperimentalMaterial3Api warning）

输出要求：
1. 自动创建完整项目结构
2. 自动生成所有核心代码文件
3. 保证项目可以直接 build 成功
4. 先完成框架，不需要复杂业务逻辑
5. 对每个文件添加必要注释
6. 不要省略关键代码
7. 如果代码较长，请分文件输出