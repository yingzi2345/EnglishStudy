-- 单词内容增强：词根词缀 + 近义反义 + 变形
ALTER TABLE tb_word
    ADD COLUMN root        VARCHAR(100) DEFAULT NULL COMMENT '词根词缀' AFTER category,
    ADD COLUMN synonyms    VARCHAR(500) DEFAULT NULL COMMENT '近义词（逗号分隔）' AFTER root,
    ADD COLUMN antonyms    VARCHAR(500) DEFAULT NULL COMMENT '反义词（逗号分隔）' AFTER synonyms,
    ADD COLUMN word_forms  VARCHAR(500) DEFAULT NULL COMMENT '单词变形（逗号分隔）' AFTER antonyms;

-- 补充高频词词根词缀数据
UPDATE tb_word SET root='col-共同+labor劳动+ate动词后缀', synonyms='cooperate,cooperate', antonyms='', word_forms='collaboration,collaborative,collaborator' WHERE word='collaborate';
UPDATE tb_word SET root='ab-离开+stract拉', synonyms='extract,remove', antonyms='add,append', word_forms='abstraction,abstractly' WHERE word='abstract';
UPDATE tb_word SET root='adapt适应', synonyms='adjust,modify', antonyms='', word_forms='adaptation,adaptable,adapter' WHERE word='adapt';
UPDATE tb_word SET root='a-加强+scend爬', synonyms='climb,rise', antonyms='descend,fall', word_forms='ascendant,ascension' WHERE word='ascend';
UPDATE tb_word SET root='ana-类似+log说+y', synonyms='comparison,similarity', antonyms='', word_forms='analogous,analogically' WHERE word='analogy';
UPDATE tb_word SET root='', synonyms='agony,torture,suffering', antonyms='pleasure,joy', word_forms='agonized,agonizing' WHERE word='agony';
UPDATE tb_word SET root='ap-加强+plaud鼓掌', synonyms='praise,applaud', antonyms='criticize,condemn', word_forms='applause,applausive' WHERE word='applaud';
UPDATE tb_word SET root='com-共同+mence开始', synonyms='begin,start,initiate', antonyms='end,finish,conclude', word_forms='commencement,commencer' WHERE word='commence';
UPDATE tb_word SET root='com-共同+mod模式+ity', synonyms='goods,merchandise', antonyms='', word_forms='commodities' WHERE word='commodity';
UPDATE tb_word SET root='at-向+tach接触', synonyms='connect,fasten,affix', antonyms='detach,separate', word_forms='attachment,attachable' WHERE word='attach';
UPDATE tb_word SET root='bene-好+fic做+ial', synonyms='advantageous,profitable', antonyms='harmful,detrimental', word_forms='beneficially' WHERE word='beneficial';
UPDATE tb_word SET root='com-共同+pens称量+ate', synonyms='compensate,offset', antonyms='', word_forms='compensation,compensatory' WHERE word='compensate';
UPDATE tb_word SET root='de-向下+cline倾斜', synonyms='decrease,diminish,drop', antonyms='increase,rise,grow', word_forms='declination,declinable' WHERE word='decline';
UPDATE tb_word SET root='e-出+valu价值+ate', synonyms='assess,estimate,evaluate', antonyms='', word_forms='evaluation,evaluative,evaluator' WHERE word='evaluate';
UPDATE tb_word SET root='ex-出+ped脚+ite', synonyms='hasten,accelerate,quicken', antonyms='delay,slow,postpone', word_forms='expedition,expeditious' WHERE word='expedite';
UPDATE tb_word SET root='', synonyms='fluctuate,oscillate,waver', antonyms='stabilize,remain', word_forms='fluctuation,fluctuant' WHERE word='fluctuate';
UPDATE tb_word SET root='', synonyms='produce,generate,yield', antonyms='consume,destroy', word_forms='generation,generator' WHERE word='generate';
UPDATE tb_word SET root='', synonyms='authentic,actual,real', antonyms='hypothetical,theoretical', word_forms='historically,historian' WHERE word='historical';
UPDATE tb_word SET root='in-不+herent黏附', synonyms='intrinsic,inborn,natural', antonyms='acquired,external', word_forms='inherently,inherence' WHERE word='inherent';
UPDATE tb_word SET root='', synonyms='justify,explain,excuse', antonyms='condemn,criticize', word_forms='justification,justifiable' WHERE word='justify';
UPDATE tb_word SET root='', synonyms='combine,merge,unite', antonyms='separate,divide', word_forms='integration,integrated' WHERE word='integrate';
UPDATE tb_word SET root='', synonyms='diminish,lessen,reduce', antonyms='increase,enhance', word_forms='mitigation,mitigating' WHERE word='mitigate';
UPDATE tb_word SET root='', synonyms='browse,skim,scan', antonyms='', word_forms='perusal,peruser' WHERE word='peruse';
UPDATE tb_word SET root='', synonyms='precede,forego,lead', antonyms='follow,succeed', word_forms='preliminary' WHERE word='preliminary';
UPDATE tb_word SET root='', synonyms='flourish,thrive,prosper', antonyms='decline,fail', word_forms='prosperity,prosperous' WHERE word='prosper';
UPDATE tb_word SET root='', synonyms='careful,cautious,prudent', antonyms='careless,reckless', word_forms='prudently,prudence' WHERE word='prudent';
UPDATE tb_word SET root='re-再+cess走+ion', synonyms='withdrawal,retreat,departure', antonyms='advance,approach', word_forms='recessionary' WHERE word='recession';
UPDATE tb_word SET root='', synonyms='tough,strong,sturdy', antonyms='fragile,delicate', word_forms='robustly,robustness' WHERE word='robust';
UPDATE tb_word SET root='', synonyms='cautious,careful,wary', antonyms='careless,rash', word_forms='scrupulously,scrupulousness' WHERE word='scrupulous';
UPDATE tb_word SET root='sub-下+sequ跟随+ent', synonyms='consecutive,successive,following', antonyms='preceding,prior', word_forms='subsequently,subsequence' WHERE word='subsequent';
UPDATE tb_word SET root='', synonyms='flexible,adaptable,versatile', antonyms='rigid,inflexible', word_forms='versatility,versatilely' WHERE word='versatile';
UPDATE tb_word SET root='', synonyms='eager,enthusiastic,passionate', antonyms='indifferent,apathetic', word_forms='zealously,zealousness' WHERE word='zealous';
UPDATE tb_word SET root='', synonyms='improve,enhance,upgrade', antonyms='worsen,deteriorate', word_forms='amelioration,ameliorative' WHERE word='ameliorate';
UPDATE tb_word SET root='', synonyms='bold,courageous,brave', antonyms='timid,cowardly', word_forms='audacity,audaciously' WHERE word='audacious';
UPDATE tb_word SET root='', synonyms='unbiased,neutral,fair', antonyms='biased,prejudiced', word_forms='impartiality,impartially' WHERE word='impartial';
UPDATE tb_word SET root='', synonyms='inescapable,unavoidable,certain', antonyms='avoidable,uncertain', word_forms='inevitably,inevitability' WHERE word='inevitable';
UPDATE tb_word SET root='', synonyms='diligent,hardworking,industrious', antonyms='lazy,idle', word_forms='sedulously,sedulousness' WHERE word='sedulous';
UPDATE tb_word SET root='', synonyms='abundant,plentiful,ample', antonyms='scarce,sparse', word_forms='copiously,copiousness' WHERE word='copious';
UPDATE tb_word SET root='', synonyms='calm,peaceful,serene', antonyms='turbulent,chaotic', word_forms='tranquility,tranquilly' WHERE word='tranquil';
UPDATE tb_word SET root='', synonyms='thrifty,economical,saving', antonyms='wasteful,extravagant', word_forms='frugality,frugally' WHERE word='frugal';
UPDATE tb_word SET root='', synonyms='elaborate,complex,intricate', antonyms='simple,plain', word_forms='ornamentation,ornamented' WHERE word='ornate';
UPDATE tb_word SET root='', synonyms='concise,brief,short', antonyms='verbose,lengthy', word_forms='tersely,terseness' WHERE word='terse';
UPDATE tb_word SET root='', synonyms='gloomy,bleak,dismal', antonyms='bright,cheerful', word_forms='morosely,moroseness' WHERE word='morose';
UPDATE tb_word SET root='', synonyms='talkative,chatty,loquacious', antonyms='quiet,taciturn', word_forms='garrulously,garrulousness' WHERE word='garrulous';
UPDATE tb_word SET root='', synonyms='harsh,rough,gruff', antonyms='gentle,smooth', word_forms='hoarsely,hoarseness' WHERE word='hoarse';
UPDATE tb_word SET root='', synonyms='cunning,sneaky,deceptive', antonyms='honest,frank', word_forms='slyly,slyness' WHERE word='sly';
UPDATE tb_word SET root='', synonyms='lively,vibrant,animated', antonyms='dull,lifeless', word_forms='vividly,vividness' WHERE word='vivid';
UPDATE tb_word SET root='', synonyms='careful,meticulous,thorough', antonyms='careless,negligent', word_forms='painstakingly' WHERE word='painstaking';
UPDATE tb_word SET root='', synonyms='abundant,overflowing,plentiful', antonyms='scarce,deficient', word_forms='exuberantly,exuberance' WHERE word='exuberant';
UPDATE tb_word SET root='', synonyms='cautious,watchful,alert', antonyms='careless,unwary', word_forms='warily,wariness' WHERE word='wary';
UPDATE tb_word SET root='', synonyms='gloomy,sullen,melancholy', antonyms='cheerful,joyful', word_forms='morosely,moroseness' WHERE word='morose';
