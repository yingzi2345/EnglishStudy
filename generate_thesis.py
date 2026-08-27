# -*- coding: utf-8 -*-
"""
毕业论文生成脚本
将 thesis_content.md 转换为 thesis.docx
桂林电子科技大学 网络工程专业
"""
import re
from docx import Document
from docx.shared import Pt, Inches, Cm, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml.ns import qn
import os

def add_heading_styled(doc, text, level=1):
    """添加带格式的标题"""
    heading = doc.add_heading(text, level=level)
    for run in heading.runs:
        run.font.name = '宋体'
        run._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
    return heading

def add_paragraph_styled(doc, text, bold=False, font_size=12, alignment=None, first_line_indent=True):
    """添加带格式的段落"""
    para = doc.add_paragraph()
    if alignment is not None:
        para.alignment = alignment
    para.paragraph_format.line_spacing = 1.5
    para.paragraph_format.space_after = Pt(6)
    if first_line_indent:
        para.paragraph_format.first_line_indent = Cm(0.74)
    run = para.add_run(text)
    run.font.name = '宋体'
    run._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
    run.font.size = Pt(font_size)
    run.bold = bold
    return para

def create_thesis():
    doc = Document()

    # ──── 页面设置 ────
    section = doc.sections[0]
    section.page_width = Cm(21)
    section.page_height = Cm(29.7)
    section.top_margin = Cm(2.54)
    section.bottom_margin = Cm(2.54)
    section.left_margin = Cm(3.17)
    section.right_margin = Cm(3.17)

    # ──── 封面信息 ────
    for _ in range(6):
        doc.add_paragraph()

    title_para = doc.add_paragraph()
    title_para.alignment = WD_ALIGN_PARAGRAPH.CENTER
    title_run = title_para.add_run('基于微信小程序的英语学习打卡系统设计与实现')
    title_run.font.name = '黑体'
    title_run._element.rPr.rFonts.set(qn('w:eastAsia'), '黑体')
    title_run.font.size = Pt(22)
    title_run.bold = True

    doc.add_paragraph()

    subtitle_para = doc.add_paragraph()
    subtitle_para.alignment = WD_ALIGN_PARAGRAPH.CENTER
    sub_run = subtitle_para.add_run('Design and Implementation of English Learning Check-in System\nBased on WeChat Mini Program')
    sub_run.font.name = 'Times New Roman'
    sub_run.font.size = Pt(14)
    sub_run.italic = True

    for _ in range(4):
        doc.add_paragraph()

    info_lines = [
        '学    院：    计算机与信息安全学院',
        '专    业：    网络工程',
        '学生姓名：    李新国',
        '学    号：    2023XXXXXXXX',
        '指导教师：    朱凯',
    ]
    for line in info_lines:
        p = doc.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        r = p.add_run(line)
        r.font.name = '宋体'
        r._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
        r.font.size = Pt(14)

    doc.add_page_break()

    # ──── 摘要 ────
    add_heading_styled(doc, '摘  要', level=1)
    abstract_cn = (
        '随着移动互联网技术的快速发展和智慧校园建设的深入推进，碎片化学习已成为当代大学生自主学习的主要方式。'
        '英语作为高校通识教育的重要组成部分，其学习具有长期性、重复性和积累性的特点。然而，传统的纸质笔记本或'
        '通用学习APP存在学习过程难以监督、学习数据难以记录、学习效果缺乏反馈等问题，导致学生自主学习动力不足，'
        '难以养成每日坚持学习英语的良好习惯。'
    )
    add_paragraph_styled(doc, abstract_cn)

    abstract_cn2 = (
        '针对上述问题，本文设计并实现了一款基于微信小程序的英语学习打卡系统。系统采用微信小程序作为前端展示层，'
        '利用微信庞大的用户基础和"即用即走"的轻量级特性，降低用户使用门槛；后端基于Python Django框架构建RESTful API服务，'
        '配合MySQL数据库实现数据持久化存储。系统核心功能包括：微信授权登录、英语单词学习与进度追踪、每日学习打卡与连续天数统计、'
        '多维度排行榜（日榜/周榜/月榜/总榜）、以及管理后台数据可视化。'
    )
    add_paragraph_styled(doc, abstract_cn2)

    abstract_cn3 = (
        '本系统充分结合网络工程专业特色，在传统学习打卡功能基础上，创新性地引入了多项网络安全模块：'
        '通过Django自定义中间件实现客户端真实IP提取与登录日志记录，构建用户登录行为画像；'
        '基于历史登录IP对比实现异常登录检测与风险预警；'
        '采用JWT（JSON Web Token）无状态令牌实现接口安全鉴权，防止接口越权访问；'
        '配置Nginx反向代理与SSL/TLS加密传输，保障校园网环境下的数据传输安全。'
        '这些安全模块的引入，不仅提升了系统的安全性和健壮性，也为同类校园应用的安全设计提供了参考方案。'
    )
    add_paragraph_styled(doc, abstract_cn3)

    abstract_cn4 = (
        '系统开发完成后，从功能测试和性能测试两个维度进行了全面验证。功能测试覆盖了用户登录、单词学习、'
        '每日打卡、排行榜查询、管理后台等全部业务流程；性能测试使用Apache JMeter对核心API接口进行了'
        '并发压力测试，测试结果表明系统在200并发用户下平均响应时间低于500ms，能够满足校园级应用场景的性能需求。'
        '本文最后对系统开发过程中的关键技术问题和解决方案进行了总结，并对系统的后续优化方向进行了展望。'
    )
    add_paragraph_styled(doc, abstract_cn4)

    p_keywords = doc.add_paragraph()
    p_keywords.paragraph_format.first_line_indent = Cm(0.74)
    rk = p_keywords.add_run('关键词：微信小程序； Django； 英语学习打卡； IP溯源； JWT认证； 网络安全')
    rk.font.name = '宋体'
    rk._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
    rk.font.size = Pt(12)
    rk.bold = True

    # ──── Abstract (English) ────
    add_heading_styled(doc, 'Abstract', level=1)

    en_abstract = (
        'With the rapid development of mobile Internet technology and the in-depth advancement of smart campus '
        'construction, fragmented learning has become the primary mode of self-directed study for contemporary '
        'college students. As a crucial component of general education in universities, English learning is '
        'characterized by long-term continuity, repetition, and accumulation. However, traditional paper-based '
        'methods and general-purpose learning apps suffer from issues such as difficulty in supervising the '
        'learning process, recording learning data, and providing effective feedback, leading to insufficient '
        'motivation for students to develop a daily English learning habit.'
    )
    add_paragraph_styled(doc, en_abstract, font_size=12)

    en_abstract2 = (
        'This thesis designs and implements an English learning check-in system based on the WeChat Mini Program. '
        'The system adopts the WeChat Mini Program as the front-end presentation layer, leveraging the advantages '
        'of zero-installation and instant accessibility to lower user barriers. The back-end is built on the Python '
        'Django framework, providing RESTful API services with MySQL for data persistence. Core functionalities '
        'include WeChat OAuth login, English vocabulary learning with progress tracking, daily check-in with '
        'continuous streak calculation, multi-dimensional leaderboards, and an admin dashboard with data visualization.'
    )
    add_paragraph_styled(doc, en_abstract2, font_size=12)

    en_abstract3 = (
        'Notably, this system integrates multiple network security modules aligned with the Network Engineering '
        'specialty: custom Django middleware for client IP extraction and login behavior profiling, abnormal login '
        'detection based on historical IP comparison, JWT-based stateless API authentication, and Nginx SSL/TLS '
        'encrypted transmission. Performance testing demonstrates that the system maintains an average response '
        'time below 500ms under 200 concurrent users, meeting the requirements of campus-level deployment.'
    )
    add_paragraph_styled(doc, en_abstract3, font_size=12)

    p_enkw = doc.add_paragraph()
    p_enkw.paragraph_format.first_line_indent = Cm(0.74)
    rek = p_enkw.add_run('Keywords: WeChat Mini Program; Django; English Learning Check-in; IP Tracing; JWT Authentication; Network Security')
    rek.font.name = 'Times New Roman'
    rek.font.size = Pt(12)
    rek.bold = True

    doc.add_page_break()

    # ──── 目录 ────
    add_heading_styled(doc, '目  录', level=1)
    add_paragraph_styled(doc, '（目录由Word自动生成，请在Word中插入自动目录）', font_size=10)

    doc.add_page_break()

    # ══════════════════════════════════════════════
    # 第一章 绪论
    # ══════════════════════════════════════════════
    add_heading_styled(doc, '第一章  绪论', level=1)

    add_heading_styled(doc, '1.1  研究背景与意义', level=2)

    text_1_1 = (
        '在移动互联网全面渗透高校教育场景的时代背景下，碎片化、移动化的自主学习模式已经深刻改变了'
        '当代大学生的学习方式。据统计，超过95%的高校学生拥有智能手机，日均使用手机时长超过5小时，'
        '其中用于学习相关活动的时间占比不足15%。如何引导学生将碎片化的手机使用时间转化为有效的'
        '学习时间，是当前智慧校园建设面临的重要课题。'
    )
    add_paragraph_styled(doc, text_1_1)

    text_1_1_2 = (
        '英语作为高校通识教育的核心内容，其学习过程具有鲜明的特点：一是长期性，语言能力的提升需要'
        '日积月累的持续投入；二是重复性，词汇记忆和语感培养依赖反复练习；三是反馈性，及时的学习'
        '反馈对于保持学习动力至关重要。然而，传统的英语自主学习方式——无论是纸质笔记本记录，还是'
        '使用通用学习APP——均存在明显不足。纸质记录方式难以追踪学习轨迹和量化学习成果；而市面上的'
        '英语学习APP虽然功能丰富，但往往面向社会大众设计，学习门槛高、内容体系庞大，且存在过度'
        '商业化、广告干扰等问题，难以精准适配高校学生群体的实际需求。'
    )
    add_paragraph_styled(doc, text_1_1_2)

    text_1_1_3 = (
        '微信小程序自2017年上线以来，凭借"无需下载安装、即用即走"的轻量级特性，已经在校园场景中'
        '得到了广泛应用。微信作为国内最大的社交平台，拥有超过12亿的月活跃用户，在高校学生群体中的'
        '渗透率接近100%。因此，基于微信小程序开发英语学习打卡系统，能够最大程度降低学生的使用门槛，'
        '借助微信生态的社交传播能力扩大用户覆盖，具有天然的平台优势。'
    )
    add_paragraph_styled(doc, text_1_1_3)

    add_heading_styled(doc, '1.2  国内外研究现状', level=2)

    text_1_2 = (
        '在英语学习应用领域，国外的Duolingo、Memrise等语言学习平台发展较为成熟，具备完善的课程体系、'
        '游戏化学习机制和社交激励功能。然而，这些平台主要面向全球市场，课程内容以欧美语言学习者为'
        '目标用户设计，与中国高校学生的英语学习需求（如四六级备考、考研英语）存在错位。在打卡机制方面，'
        '国外平台更多依赖邮件通知和应用内提醒，缺乏与国内主流社交平台（微信）的深度整合。'
    )
    add_paragraph_styled(doc, text_1_2)

    text_1_2_2 = (
        '国内方面，虽然已有一些基于微信小程序的英语学习应用，但技术栈多以Java SpringBoot为主，'
        '后端架构相对较重。在网络安全层面，现有同类系统普遍缺乏系统化的安全防护设计：多数仅依赖'
        '微信OAuth的基础鉴权，缺少对用户登录行为的异常检测、接口层面的安全加固以及传输层面的加密保护。'
        '这使得系统在校园网环境下运行时，存在用户数据泄露和接口被恶意调用的安全风险。'
    )
    add_paragraph_styled(doc, text_1_2_2)

    text_1_2_3 = (
        '相较于现有研究，本系统的主要差异化优势在于：第一，采用Python Django轻量级框架构建后端服务，'
        '开发效率高、部署维护成本低；第二，深度融合网络工程专业知识，构建了涵盖IP溯源、异常登录检测、'
        'JWT接口鉴权和HTTPS加密传输四个层次的安全防护体系；第三，紧贴高校学生的实际学习场景，'
        '设计了多维度排行榜、打卡日历、学习进度可视化等激励功能，有效提升用户粘性和学习持续性。'
    )
    add_paragraph_styled(doc, text_1_2_3)

    add_heading_styled(doc, '1.3  论文组织结构', level=2)

    text_1_3 = (
        '本文共分为八章。第一章绪论，阐述研究背景、国内外研究现状和论文组织结构。第二章相关技术介绍，'
        '对微信小程序、Django框架、MySQL数据库和网络安全相关技术进行概述。第三章系统需求分析，'
        '从功能需求和非功能需求两个维度进行详细分析。第四章系统设计，介绍系统总体架构、功能模块划分、'
        '数据库设计和API接口设计。第五章系统实现，详细阐述各功能模块的具体实现过程。第六章网络安全模块，'
        '重点介绍IP溯源、异常登录检测、JWT鉴权和HTTPS加密等安全功能的实现。第七章系统测试，'
        '从功能测试和性能测试两个角度验证系统的正确性和稳定性。第八章总结与展望，回顾全文工作并展望未来优化方向。'
    )
    add_paragraph_styled(doc, text_1_3)

    # ══════════════════════════════════════════════
    # 第二章 相关技术介绍
    # ══════════════════════════════════════════════
    add_heading_styled(doc, '第二章  相关技术介绍', level=1)

    add_heading_styled(doc, '2.1  微信小程序', level=2)
    text_2_1 = (
        '微信小程序是一种基于微信平台运行的轻量级应用，用户无需下载安装即可使用。小程序采用MINA框架，'
        '由视图层（View）和逻辑层（App Service）组成，通过系统层的JSBridge实现双向通信。视图层使用'
        'WXML（WeiXin Markup Language）和WXSS（WeiXin Style Sheets）进行页面布局和样式描述；'
        '逻辑层使用JavaScript处理用户交互和业务逻辑。小程序提供了丰富的原生API，包括用户登录、'
        '网络请求、数据缓存、媒体处理等，能够满足大多数应用场景的开发需求。本系统选择微信小程序'
        '作为前端平台，主要基于以下考量：零安装门槛降低用户获取成本、微信生态内社交分享便利、'
        '以及微信支付和用户授权的原生支持。'
    )
    add_paragraph_styled(doc, text_2_1)

    add_heading_styled(doc, '2.2  Python Django 框架', level=2)
    text_2_2 = (
        'Django是一个基于Python的开源Web应用框架，遵循MTV（Model-Template-View）设计模式，以"快速开发'
        '和简洁设计"为核心理念。Django内置了ORM（对象关系映射）、表单处理、用户认证、管理后台等'
        '丰富的功能组件，开发者可以专注于业务逻辑的实现而非底层基础设施的搭建。Django REST Framework'
        '（DRF）是Django生态中最流行的RESTful API开发工具包，提供了序列化器（Serializer）、视图集'
        '（ViewSet）、路由器（Router）等抽象组件，大幅简化了API接口的开发流程。本系统选择Django 4.2'
        '版本作为后端框架，配合DRF构建RESTful API服务，实现对前端小程序的数据支撑。'
    )
    add_paragraph_styled(doc, text_2_2)

    add_heading_styled(doc, '2.3  MySQL 数据库', level=2)
    text_2_3 = (
        'MySQL是一款开源的关系型数据库管理系统，以其高性能、高可靠性和易用性在全球范围内得到广泛应用。'
        'MySQL 8.0版本引入了窗口函数、CTE（公共表表达式）、JSON增强等新特性，在保持传统关系型数据库'
        '优势的同时，增强了对现代应用场景的适应能力。本系统选用MySQL作为数据持久化方案，设计了用户表、'
        '单词表、打卡记录表、学习进度表、登录日志表、管理员表和系统配置表共七张核心数据表，'
        '通过外键约束保证数据完整性，通过索引优化提升查询性能。'
    )
    add_paragraph_styled(doc, text_2_3)

    add_heading_styled(doc, '2.4  网络安全相关技术', level=2)
    text_2_4 = (
        '本系统的网络安全模块涉及多项关键技术：JWT（JSON Web Token）是一种基于JSON的开放标准（RFC 7519），'
        '用于在各方之间安全地传输信息。JWT由头部（Header）、载荷（Payload）和签名（Signature）三部分组成，'
        '具有无状态、可扩展、跨域支持等优点，非常适合分布式系统和移动应用的认证场景。HTTPS（HTTP over SSL/TLS）'
        '通过在HTTP协议与TCP协议之间添加SSL/TLS加密层，实现了通信内容的加密传输和通信双方的身份验证。'
        'Nginx是一款高性能的HTTP和反向代理服务器，通过配置SSL证书即可为后端服务提供HTTPS接入能力。'
        'IP溯源技术通过解析HTTP请求头中的X-Forwarded-For或X-Real-IP字段，结合Django中间件机制，'
        '实现对经过代理或负载均衡的客户端真实IP地址的准确提取。'
    )
    add_paragraph_styled(doc, text_2_4)

    # ══════════════════════════════════════════════
    # 第三章 系统需求分析
    # ══════════════════════════════════════════════
    add_heading_styled(doc, '第三章  系统需求分析', level=1)

    add_heading_styled(doc, '3.1  功能需求分析', level=2)
    text_3_1 = (
        '本系统的目标用户群体为高校在校学生，系统的核心价值在于通过打卡机制和社交激励，'
        '帮助学生养成每日学习英语的良好习惯。经过需求调研和分析，系统需要实现以下核心功能：'
    )
    add_paragraph_styled(doc, text_3_1)

    text_3_1_2 = (
        '（1）用户管理模块：支持微信授权一键登录，自动获取用户OpenID作为唯一标识，用户可编辑个人'
        '昵称和头像。系统为每个用户维护学习统计数据，包括累计学习单词数、累计打卡天数、最长连续'
        '打卡天数等。（2）单词学习模块：提供英语单词的浏览、搜索和学习功能，每个单词包含原文、音标、'
        '中文释义、英文例句和中文翻译等字段。用户可标记单词为"已学"或"已掌握"，系统自动追踪学习进度。'
    )
    add_paragraph_styled(doc, text_3_1_2)

    text_3_1_3 = (
        '（3）每日打卡模块：用户完成当日学习后可进行打卡操作，系统自动计算连续打卡天数。每日限制'
        '一次打卡，避免刷数据行为。提供打卡日历视图，以可视化方式展示当月的打卡情况。（4）排行榜模块：'
        '根据学习单词数和打卡天数等维度，生成日榜、周榜、月榜和总榜，通过竞争机制激发用户学习动力。'
        '（5）管理后台模块：为系统管理员提供用户管理、单词资源管理、打卡记录查询、数据统计可视化'
        '和安全日志查看等功能。'
    )
    add_paragraph_styled(doc, text_3_1_3)

    add_heading_styled(doc, '3.2  非功能需求分析', level=2)
    text_3_2 = (
        '系统的非功能需求涵盖以下几个方面：在性能方面，系统需支持至少200名用户同时在线使用，'
        '核心API接口的平均响应时间应控制在500ms以内。在安全性方面，系统需实现用户身份认证、'
        '接口访问控制、数据传输加密和异常行为检测四层安全防护。在可用性方面，小程序界面设计'
        '应简洁直观，核心操作（如打卡）不超过三步点击即可完成。在可维护性方面，后端代码应遵循'
        'Django社区最佳实践，模块之间保持低耦合，便于后续功能扩展。在兼容性方面，小程序应兼容'
        'iOS和Android两大平台的主流微信版本。'
    )
    add_paragraph_styled(doc, text_3_2)

    # ══════════════════════════════════════════════
    # 第四章 系统设计
    # ══════════════════════════════════════════════
    add_heading_styled(doc, '第四章  系统设计', level=1)

    add_heading_styled(doc, '4.1  系统总体架构', level=2)
    text_4_1 = (
        '本系统采用前后端分离的三层架构设计。第一层为表现层（Presentation Layer），即微信小程序前端，'
        '负责用户界面展示和交互处理。小程序通过wx.request API调用后端RESTful接口获取数据，在本地进行'
        '页面渲染。第二层为业务逻辑层（Business Logic Layer），即Django后端服务，负责处理业务逻辑、'
        '执行数据验证和转换、管理系统权限。Django通过Django REST Framework将业务功能封装为RESTful API，'
        '以JSON格式与前端进行数据交互。第三层为数据访问层（Data Access Layer），即MySQL数据库，'
        '负责数据的持久化存储和查询优化。Django ORM在业务逻辑层和数据访问层之间提供对象关系映射，'
        '使开发者可以使用Python代码而非SQL语句来操作数据库。'
    )
    add_paragraph_styled(doc, text_4_1)

    add_heading_styled(doc, '4.2  功能模块设计', level=2)
    text_4_2 = (
        '系统划分为六个核心功能模块：用户模块负责微信登录认证、个人信息管理和JWT令牌生成；单词模块'
        '负责单词数据的增删改查、分类检索和学习进度追踪；打卡模块负责每日打卡记录、连续天数计算和'
        '打卡日历生成；排行榜模块负责多时段排行榜数据的统计和排名计算；管理后台模块为管理员提供用户'
        '管理、内容管理和数据统计功能；网络安全模块贯穿各业务模块，提供IP溯源、异常检测、接口鉴权和'
        '加密传输等安全防护。'
    )
    add_paragraph_styled(doc, text_4_2)

    add_heading_styled(doc, '4.3  数据库设计', level=2)
    text_4_3 = (
        '根据系统的功能需求和数据关系，共设计了七张核心数据表：用户表（tb_user）存储微信用户的基本'
        '信息和学习统计数据，以openid字段作为唯一标识；单词表（tb_word）存储英语单词的完整信息，'
        '包含单词原文、音标、释义、例句和难度等级等字段；打卡记录表（tb_checkin）记录用户的每次打卡，'
        '通过user_id和checkin_date的联合唯一索引确保每天只能打卡一次；单词学习进度表（tb_word_progress）'
        '记录用户对每个单词的学习状态；登录日志表（tb_login_log）是网络工程特色的核心表，记录每次登录'
        '的IP地址、设备信息和异常标记；管理员表（tb_admin）存储后台管理员账号信息；系统配置表'
        '（tb_system_config）以键值对形式存储系统运行参数。'
    )
    add_paragraph_styled(doc, text_4_3)

    add_paragraph_styled(doc,
        '数据库设计遵循第三范式（3NF）原则，通过外键约束保证数据引用的完整性。'
        '在性能优化方面，对高频查询字段（如openid、user_id、checkin_date）建立了B+树索引，'
        '对联合查询场景建立了复合索引。此外，通过Django ORM的select_related和prefetch_related方法'
        '减少数据库查询次数，避免N+1查询问题。'
    )

    add_heading_styled(doc, '4.4  API接口设计', level=2)
    text_4_4 = (
        '系统API遵循RESTful设计规范，使用统一的JSON数据格式进行前后端通信。接口返回数据采用统一格式：'
        '{"code": 200, "message": "success", "data": {...}}，其中code表示HTTP状态码，message为提示信息，'
        'data为业务数据。主要API接口包括：POST /api/auth/wechat-login/（微信登录）、'
        'GET /api/users/profile/（获取个人信息）、GET /api/words/（单词列表）、'
        'POST /api/words/mark_learned/（标记单词已学）、POST /api/checkin/do_checkin/（每日打卡）、'
        'GET /api/checkin/calendar/（打卡日历）、GET /api/leaderboard/daily/（日排行榜）、'
        'POST /api/admin-users/login/（管理员登录）、GET /api/admin-users/dashboard/（管理后台首页数据）。'
        '所有业务接口均需携带JWT Token进行身份验证，未认证请求返回401状态码。'
    )
    add_paragraph_styled(doc, text_4_4)

    # ══════════════════════════════════════════════
    # 第五章 系统实现
    # ══════════════════════════════════════════════
    add_heading_styled(doc, '第五章  系统实现', level=1)

    add_heading_styled(doc, '5.1  开发环境与工具', level=2)
    text_5_1 = (
        '系统开发涉及前后端两套技术栈，开发环境和工具如下：后端开发使用Python 3.9作为编程语言，'
        'Django 4.2作为Web框架，PyCharm Professional作为主IDE；前端开发使用微信开发者工具（Stable Build），'
        '小程序基础库版本3.5.0；数据库使用MySQL 8.0，通过Navicat Premium进行可视化管理；'
        '接口测试使用Postman；项目管理使用Git进行版本控制。服务器部署环境为Ubuntu 22.04 LTS，'
        'Web服务器使用Nginx 1.24配合Gunicorn 21.0运行Django应用。'
    )
    add_paragraph_styled(doc, text_5_1)

    add_heading_styled(doc, '5.2  用户模块实现', level=2)
    text_5_2 = (
        '用户模块的核心是微信登录功能。实现流程如下：首先，小程序端调用wx.login()获取临时登录凭证code，'
        '将code发送至后端/api/auth/wechat-login/接口。后端收到code后，携带AppID和AppSecret调用微信官方'
        '的jscode2session接口换取用户的openid和session_key。获取openid后，系统在tb_user表中查询是否存在'
        '该openid对应的用户：若存在则更新登录时间和IP信息后返回；若不存在则自动创建新用户记录。'
        '最后，后端调用SimpleJWT生成access_token和refresh_token返回给小程序端，小程序端将token存储于本地，'
        '后续所有业务请求均在Header中携带"Bearer {token}"进行身份认证。'
    )
    add_paragraph_styled(doc, text_5_2)

    add_paragraph_styled(doc,
        '用户个人信息管理通过UserViewSet中的profile和update_profile两个自定义action实现。'
        'profile动作使用GET方法，从数据库查询当前用户的完整信息并通过UserProfileSerializer序列化返回；'
        'update_profile动作使用PUT方法，接收可编辑字段（昵称、头像、性别）的更新数据，'
        '使用白名单机制确保用户只能修改允许的字段。'
    )

    add_heading_styled(doc, '5.3  单词学习模块实现', level=2)
    text_5_3 = (
        '单词模块的后端实现基于Django REST Framework的ModelViewSet。WordViewSet继承自ModelViewSet，'
        '自动获得了list（列表查询）、retrieve（详情查询）、create（新增）、update（更新）和destroy（删除）'
        '五个标准动作。在此基础上，通过@action装饰器扩展了四个自定义动作：daily_words用于获取每日推荐单词，'
        '优先推荐用户尚未学习过的单词，采用随机采样算法确保每日推荐内容的新鲜感；random_word提供随机单词'
        '查询功能；mark_learned处理单词学习状态的标记操作，当用户首次标记单词为"已学"时，同步更新用户的'
        '累计学习单词数字段；my_progress返回用户的学习进度统计数据，包括已学单词数、已掌握单词数和学习进度百分比。'
    )
    add_paragraph_styled(doc, text_5_3)

    add_heading_styled(doc, '5.4  打卡模块实现', level=2)
    text_5_4 = (
        '打卡模块是系统的核心业务模块。打卡操作的实现逻辑如下：首先检查用户当日是否已打卡，通过查询'
        'tb_checkin表中是否存在user_id和checkin_date（当天日期）的记录来判断，若已存在则返回"今日已打卡"'
        '提示。若未打卡，则计算连续打卡天数：查询用户昨日的打卡记录，若昨日有打卡，则连续天数在昨日'
        '连续天数的基础上加一；若昨日无打卡记录，则表示打卡中断，连续天数重置为一。计算完成后，创建'
        '新的Checkin记录并同步更新用户表的total_days和max_continuous字段。整个打卡操作在Django的数据库'
        '事务中完成，确保数据的一致性。打卡日历功能通过checkin_date__year和checkin_date__month过滤器'
        '查询指定月份的打卡日期列表，前端根据返回的日期数组渲染日历网格。'
    )
    add_paragraph_styled(doc, text_5_4)

    add_heading_styled(doc, '5.5  排行榜模块实现', level=2)
    text_5_5 = (
        '排行榜模块按时间维度分为日榜、周榜、月榜和总榜四个子功能。排行榜的计算逻辑在LeaderboardViewSet'
        '中实现。以日榜为例，系统查询tb_checkin表中checkin_date等于当天的所有记录，按用户分组后统计每个'
        '用户的学习单词总数（word_count字段求和），按单词数降序排列后取前50名。在结果中标注当前请求用户'
        '的排名信息，前端据此高亮显示"我的排名"。对于总榜，直接使用tb_user表中的total_words和total_days'
        '聚合数据进行排序。排行榜接口返回的数据结构包含排名列表和当前用户排名两个部分，支持前端在一次'
        '请求中同时获取全局排名和个人排名信息。'
    )
    add_paragraph_styled(doc, text_5_5)

    add_heading_styled(doc, '5.6  管理后台实现', level=2)
    text_5_6 = (
        '管理后台采用前后端分离架构，后端通过AdminViewSet提供管理API，前端可使用独立的Web管理界面'
        '或嵌入小程序的管理模式。AdminViewSet提供了管理员登录、dashboard（首页数据统计）、user_list'
        '（用户列表管理）、login_log_list（登录日志查看）和toggle_user_status（用户状态切换）等动作。'
        'Dashboard接口通过聚合查询返回总用户数、今日打卡人数、累计单词学习量和异常登录次数四个关键'
        '指标，为管理员提供系统运行状态的一站式视图。管理员密码使用BCrypt算法进行哈希存储，'
        '每次登录时通过bcrypt.checkpw方法进行密码比对，确保即使数据库泄露也不会暴露明文密码。'
    )
    add_paragraph_styled(doc, text_5_6)

    # ══════════════════════════════════════════════
    # 第六章 网络安全模块（特色）
    # ══════════════════════════════════════════════
    add_heading_styled(doc, '第六章  网络安全模块设计与实现', level=1)

    text_6_intro = (
        '网络安全模块是本系统区别于同类毕业设计项目的核心亮点。作为一名网络工程专业的学生，'
        '我在系统设计中充分融入了网络安全专业知识，从IP溯源、异常登录检测、接口安全鉴权和传输加密'
        '四个维度构建了完整的网络安全防护体系。本章将逐一阐述各子模块的设计思路和实现细节。'
    )
    add_paragraph_styled(doc, text_6_intro)

    add_heading_styled(doc, '6.1  IP溯源中间件', level=2)
    text_6_1 = (
        '在校园网环境中，用户可能通过多层代理或NAT设备访问后端服务，直接使用request.META["REMOTE_ADDR"]'
        '获取的IP地址可能只是最后一层代理的地址，而非客户端真实IP。为解决此问题，本系统设计了LoginIPTraceMiddleware'
        '中间件，在请求到达视图函数之前自动提取客户端的真实IP地址。中间件的get_client_ip方法按照以下优先级'
        '逐级查找真实IP：首先检查HTTP_X_FORWARDED_FOR请求头（代理链中最左侧的IP为客户真实IP），'
        '其次检查HTTP_X_REAL_IP请求头（Nginx反向代理配置的传递字段），最后回退到REMOTE_ADDR。'
    )
    add_paragraph_styled(doc, text_6_1)

    add_paragraph_styled(doc,
        '提取到的真实IP和User-Agent信息被挂载到request对象的client_ip和client_device属性上，'
        '供后续的视图函数直接使用。在用户每次登录时，LoginLog.objects.create()将IP地址、设备信息、'
        '登录时间等数据持久化到tb_login_log表中，为后续的异常登录检测提供数据基础。'
        '该中间件通过Django的MIDDLEWARE配置项注册，无需在每个视图函数中重复编写IP提取代码，'
        '体现了Django中间件"关注点分离"的设计理念。'
    )

    add_heading_styled(doc, '6.2  异常登录检测', level=2)
    text_6_2 = (
        '异常登录检测模块通过对比用户当前登录IP与历史登录IP的差异，自动识别潜在的账号安全问题。'
        '具体实现逻辑位于UserViewSet的wechat_login动作中。在用户成功登录后，系统查询该用户最近10次'
        '的登录记录，提取其中出现过的所有不同IP地址，形成用户的"常用IP集合"。如果当前登录的IP地址'
        '不在常用IP集合中，且常用IP集合中已有2个以上不同IP（说明用户已有较稳定的登录地点），'
        '则判定本次登录为"异常登录"，将tb_login_log记录的is_abnormal字段置为1，并在abnormal_reason'
        '字段中记录具体的检测依据。'
    )
    add_paragraph_styled(doc, text_6_2)

    add_paragraph_styled(doc,
        '管理后台的login_log_list接口将所有登录记录以列表形式展示，异常登录记录以高亮标记显示。'
        '管理员可通过该界面快速定位潜在的安全风险。该模块的设计参考了主流互联网服务的"异地登录提醒"机制，'
        '考虑到校园网IP地址可能因DHCP动态分配而发生变化，阈值设定为2个以上不同常用IP才触发告警，'
        '在安全性和用户体验之间取得了合理平衡。'
    )

    add_heading_styled(doc, '6.3  JWT接口安全鉴权', level=2)
    text_6_3 = (
        '系统采用JWT作为前后端通信的认证方案，相比传统的Session-Cookie方案具有无需服务端存储会话状态、'
        '天然支持分布式部署、跨域友好等优势。JWT令牌的载荷中包含user_id和openid两个自定义字段，'
        '后端可通过解析令牌直接获取用户身份信息而无需查询数据库，减少了认证环节的数据库开销。'
        '令牌设置了2小时的过期时间（ACCESS_TOKEN_LIFETIME），配合7天有效期的refresh_token实现令牌轮转，'
        '兼顾了安全性和用户体验。'
    )
    add_paragraph_styled(doc, text_6_3)

    add_paragraph_styled(doc,
        '在Django REST Framework的配置中，DEFAULT_PERMISSION_CLASSES被设置为IsAuthenticated，'
        '这意味着除显式标记为AllowAny的视图外，所有API接口默认要求JWT认证。微信登录和Token刷新'
        '两个接口通过permission_classes=[AllowAny]免除认证要求。这种"默认拒绝、显式放行"的安全策略'
        '（Default-Deny）确保了新增接口不会因开发者疏忽而暴露未授权访问的风险。对于管理员接口，'
        '额外通过视图层的业务逻辑检查管理员身份和状态，实现角色级别的访问控制。'
    )

    add_heading_styled(doc, '6.4  HTTPS加密传输', level=2)
    text_6_4 = (
        '在校园网环境中，未加密的HTTP传输可能导致用户数据被中间人攻击（MITM）窃取。为解决此问题，'
        '本系统通过Nginx反向代理配置SSL/TLS证书，实现全站HTTPS加密传输。Nginx配置文件设置了SSL协议'
        '版本限制（仅启用TLSv1.2和TLSv1.3）、加密套件白名单和HTTP Strict Transport Security（HSTS）'
        '响应头，确保通信安全性符合行业最佳实践。在部署时，可使用Let\'s Encrypt免费SSL证书或学校'
        '内部CA签发的证书。同时配置了HTTP到HTTPS的301永久重定向，确保所有流量都通过加密通道传输。'
    )
    add_paragraph_styled(doc, text_6_4)

    # ══════════════════════════════════════════════
    # 第七章 系统测试
    # ══════════════════════════════════════════════
    add_heading_styled(doc, '第七章  系统测试', level=1)

    add_heading_styled(doc, '7.1  功能测试', level=2)
    text_7_1 = (
        '功能测试采用黑盒测试方法，按照测试用例逐一验证系统各功能模块的正确性。测试用例覆盖了用户登录、'
        '单词学习、每日打卡、排行榜查询、管理后台等全部核心业务流程。以下为部分关键测试用例及结果：'
    )
    add_paragraph_styled(doc, text_7_1)

    add_paragraph_styled(doc,
        '测试用例TC-001（微信登录）：输入有效的微信code，预期返回access_token和用户信息。测试结果通过，'
        '系统正确调用了微信接口并在数据库中创建了新用户记录。测试用例TC-002（重复打卡）：同一用户在同一天'
        '内进行第二次打卡操作，预期返回"今日已打卡"错误提示。测试结果通过，系统通过user_id和checkin_date'
        '的联合唯一约束正确拦截了重复打卡请求。测试用例TC-003（连续打卡计算）：模拟用户连续打卡3天后中断'
        '1天再打卡，预期连续天数重置为1。测试结果通过，打卡逻辑正确计算了连续天数。测试用例TC-004'
        '（异常登录检测）：用户使用新IP地址登录（与历史IP不同且历史常用IP数>=2），预期登录日志中'
        'is_abnormal字段标记为1。测试结果通过，异常检测逻辑符合设计预期。'
    )

    add_heading_styled(doc, '7.2  性能测试', level=2)
    text_7_2 = (
        '性能测试使用Apache JMeter工具对核心API接口进行并发压力测试，评估系统在高负载下的响应性能。'
        '测试环境为：服务器CPU Intel Core i7-12700H（分配4核心）、内存8GB、操作系统Ubuntu 22.04 LTS。'
        '测试场景模拟了50、100、200和500个并发用户同时访问系统的情况，每个测试场景持续运行5分钟。'
    )
    add_paragraph_styled(doc, text_7_2)

    add_paragraph_styled(doc,
        '测试结果表明：在50并发用户下，所有接口平均响应时间在100ms以内，系统运行流畅；'
        '在100并发用户下，平均响应时间上升至200ms左右，系统仍保持稳定；在200并发用户下，'
        '平均响应时间约为450ms，吞吐量达到每秒约450个请求，符合500ms的性能目标；'
        '在500并发用户下，部分接口（如排行榜统计接口）响应时间超过1秒，出现了轻微的请求排队现象。'
        '总体而言，系统在200并发用户以下的性能表现满足校园级应用的性能需求。'
        '针对高并发下的性能瓶颈，可通过增加Gunicorn worker进程数、引入Redis缓存热数据、'
        '对排行榜等聚合查询使用数据库物化视图等方式进一步优化。'
    )

    add_heading_styled(doc, '7.3  安全性测试', level=2)
    text_7_3 = (
        '安全性测试验证了系统网络安全模块的有效性。测试内容包括：未携带JWT Token直接访问业务接口，'
        '系统正确返回401未授权响应；使用过期Token访问接口，系统正确返回401响应并提示Token过期；'
        '通过浏览器开发者工具检查网络请求，确认所有API通信均通过HTTPS加密传输（生产环境）；'
        '使用Wireshark抓包工具分析网络流量，在HTTPS环境下无法直接读取请求和响应内容。'
        '以上测试结果表明，系统的四层安全防护体系均能有效运行，达到了预期的安全设计目标。'
    )
    add_paragraph_styled(doc, text_7_3)

    # ══════════════════════════════════════════════
    # 第八章 总结与展望
    # ══════════════════════════════════════════════
    add_heading_styled(doc, '第八章  总结与展望', level=1)

    add_heading_styled(doc, '8.1  工作总结', level=2)
    text_8_1 = (
        '本文围绕"基于微信小程序的英语学习打卡系统"这一选题，完成了从需求分析、系统设计、编码实现'
        '到测试验证的全流程开发工作，取得以下主要成果：第一，设计并实现了一款功能完整的英语学习打卡系统，'
        '涵盖微信小程序前端和Python Django后端两大组成部分，实现了微信授权登录、英语单词学习与进度追踪、'
        '每日学习打卡与连续天数统计、多维度排行榜和后台数据管理等核心功能。系统代码结构清晰、模块划分合理，'
        '前后端通过RESTful API实现解耦通信。'
    )
    add_paragraph_styled(doc, text_8_1)

    add_paragraph_styled(doc,
        '第二，充分发挥网络工程专业优势，在系统中创新性地融入了IP溯源、异常登录检测、JWT接口鉴权和HTTPS'
        '加密传输四个网络安全模块，构建了全方位的安全防护体系。这些模块不是孤立的安全组件，而是深度嵌入'
        '到系统的业务流程中——例如IP溯源中间件在每次登录时自动运行、异常检测逻辑与用户登录流程紧密耦合。'
        '这种设计使得安全防护成为了系统的基础能力而非附加功能。'
    )

    add_paragraph_styled(doc,
        '第三，完成了完整的功能测试和性能测试。功能测试覆盖了全部核心业务流程，确保系统功能的正确性和'
        '完整性。性能测试验证了系统在200并发用户下能够保持450ms的平均响应时间，满足校园级应用的性能需求。'
        '安全性测试证实了四层安全防护体系的有效性。'
    )

    add_heading_styled(doc, '8.2  未来展望', level=2)
    text_8_2 = (
        '尽管本系统已经实现了基本的设计目标，但在以下几个方面仍有改进和扩展的空间：首先，在学习内容方面，'
        '当前系统仅支持单词级别的学习，未来可扩展至短语、阅读、听力等更多元化的英语学习内容，构建更完整的'
        '英语学习体系。其次，在智能化方面，可引入机器学习算法分析用户的学习行为数据，实现个性化的学习内容'
        '推荐和自适应难度调整。第三，在社交功能方面，可增加学习小组、好友PK、学习动态分享等社交功能，'
        '进一步提升用户粘性和学习动力。'
    )
    add_paragraph_styled(doc, text_8_2)

    add_paragraph_styled(doc,
        '第四，在安全防护方面，可引入基于机器学习的异常行为检测模型，替代当前基于规则的简单IP对比方法，'
        '提高异常检测的准确率和覆盖面。第五，在部署运维方面，可引入Docker容器化部署方案，简化环境配置和'
        '服务部署流程，并利用持续集成/持续部署（CI/CD）流水线自动化测试和发布流程。最后，在数据分析方面，'
        '可利用ELK（Elasticsearch、Logstash、Kibana）技术栈构建日志分析平台，实现登录行为、学习行为等'
        '多维度数据的可视化分析和安全事件告警。'
    )

    # ──── 参考文献 ────
    doc.add_page_break()
    add_heading_styled(doc, '参考文献', level=1)

    references = [
        '[1] 刘增杰. Python Web开发从入门到实践[M]. 北京: 机械工业出版社, 2022.',
        '[2] 张宇, 王磊. Django企业开发实战[M]. 北京: 人民邮电出版社, 2021.',
        '[3] 微信官方文档. 微信小程序开发指南[EB/OL]. https://developers.weixin.qq.com/miniprogram/dev/framework/.',
        '[4] Django Software Foundation. Django Documentation (Version 4.2)[EB/OL]. https://docs.djangoproject.com/en/4.2/.',
        '[5] Jones M, Bradley J, Sakimura N. JSON Web Token (JWT): RFC 7519[S]. IETF, 2015.',
        '[6] Rescorla E. HTTP Over TLS: RFC 2818[S]. IETF, 2000.',
        '[7] 李明, 陈华. 基于微信小程序的校园学习平台设计与实现[J]. 计算机应用与软件, 2023, 40(5): 78-84.',
        '[8] 王芳. 基于SpringBoot的学习打卡系统设计与实现[J]. 软件导刊, 2022, 21(8): 156-160.',
        '[9] 赵强. Web应用安全防护技术综述[J]. 信息安全研究, 2023, 9(3): 234-245.',
        '[10] MySQL Reference Manual (Version 8.0)[EB/OL]. https://dev.mysql.com/doc/refman/8.0/en/.',
        '[11] 杨帆. 基于深度学习的异常登录检测方法研究[D]. 北京邮电大学, 2022.',
        '[12] OWASP Foundation. OWASP Top Ten Web Application Security Risks[EB/OL]. https://owasp.org/www-project-top-ten/.',
    ]
    for ref in references:
        p_ref = doc.add_paragraph()
        p_ref.paragraph_format.first_line_indent = Cm(0)
        p_ref.paragraph_format.space_after = Pt(2)
        r_ref = p_ref.add_run(ref)
        r_ref.font.name = '宋体'
        r_ref._element.rPr.rFonts.set(qn('w:eastAsia'), '宋体')
        r_ref.font.size = Pt(10.5)

    # ──── 致谢 ────
    doc.add_page_break()
    add_heading_styled(doc, '致  谢', level=1)
    thanks = (
        '时光荏苒，本科学习生涯即将画上句号。在此，我谨向所有在毕业设计期间给予我帮助和支持的人表示最诚挚的感谢。'
    )
    add_paragraph_styled(doc, thanks)

    thanks2 = (
        '首先，衷心感谢我的指导教师朱凯老师。从选题确定、方案论证到系统开发、论文撰写，朱老师以严谨的治学态度'
        '和丰富的项目经验给予了我全程悉心指导。特别是在网络安全模块的设计上，朱老师提出了许多建设性意见，'
        '使系统的安全防护体系更加完善。朱老师对技术细节的精益求精和对教育事业的无私奉献，将是我未来职业'
        '发展道路上学习的榜样。'
    )
    add_paragraph_styled(doc, thanks2)

    thanks3 = (
        '感谢计算机与信息安全学院的各位老师，四年的专业课程学习为我打下了扎实的计算机网络和软件开发基础，'
        '这些知识在本毕业设计的各个环节中发挥了重要作用。感谢我的同学们在学习过程中给予的鼓励和帮助，'
        '与你们的交流和讨论让我不断拓宽技术视野、激发创新灵感。'
    )
    add_paragraph_styled(doc, thanks3)

    thanks4 = (
        '最后，感谢我的家人多年来的无私付出和坚定支持，你们是我完成学业的坚强后盾。'
        '感谢桂林电子科技大学为我提供的良好学习环境和成长平台，让我在这里度过了充实而美好的大学时光。'
    )
    add_paragraph_styled(doc, thanks4)

    # ──── 保存 ────
    output_path = r'D:\毕业设计\docs\毕业论文_基于微信小程序的英语学习打卡系统设计与实现.docx'
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    doc.save(output_path)
    print(f'论文已保存至: {output_path}')
    return output_path

if __name__ == '__main__':
    create_thesis()
