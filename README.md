<h3> BeanUtilHelper </h3>
<ul>
    <li>Generate GetterSetter Code with One Click.</li>
    <li>Static inspection of attribute duplication, prompting for duplication where the property name is the same but the type is inconsistent, prompting for duplication where there are no identical properties.</li>
    <li>One-click generation of property copy comments, explicitly declaring the copied properties, one-click preview of copied properties</li>
    <li>One-click generation of different property comments, one-click preview of different properties</li>
<li>Automatically identify the <code>BeanUtil.copyProperties</code>and <code>BeanUtils.copyProperties</code>
        methods.
    </li>
    <li>Automatically retrieve the common properties of the source and target types and generate line comments.</li>
    <li>Automatically convert multiple property copy comments into block comments.</li>
</ul>
<h3>Usage</h3>
<ol>
    <li>Place the cursor on the <code>BeanUtil.copyProperties</code> or <code>BeanUtils.copyProperties</code> method.
    </li>
    <li>"Invoke Intention Action ( ⌥ + Enter for Mac; Alt + Enter for Win/Linux ), select the corresponding option of 'BeanUtilHelper'".
    </li>
    <li>
        Press enter to generate comments;
    </li>
</ol>

<h3> BeanUtil助手 </h3>
<ul>
    <li>一键生成 GetterSetter 代码</li>
    <li>静态检查属性复制,提示属性名相同类型不一致的复制,提示没有相同属性的复制</li>
    <li>一键生成复制属性的注释,显式声明复制出的属性,一键预览复制的属性</li>
    <li>一键生成差异属性的注释,一键预览差异的属性,标识差异化属性,发现潜在问题</li>
    <li>自动识别 <code>BeanUtil.copyProperties</code> 与 <code>BeanUtils.copyProperties</code>方法</li>
    <li>自动获取源类型与目标类型共有属性生成行注释</li>
    <li>自动转换多属性复制注释为块注释</li>
</ul>
<h3>使用方式</h3>
<ol>
    <li>将光标放置在 <code>BeanUtil.copyProperties</code> 或 <code>BeanUtils.copyProperties</code> 方法上</li>
    <li>唤起意图动作 ( ⌥ + Enter for Mac; Alt + Enter for Win/Linux ),选择 "BeanUtilHelper"的对应选项</li>
    <li>回车生成注释或代码</li>
</ol>

差异预览
![差异预览](doc/img/差异预览.png)
静态检查
![静态检查](doc/img/静态检查-属性类型不一致.png)
![静态检查](doc/img/静态检查-没有相同属性.png)
生成 GetterSetter
![生成GetterSetter](doc/img/生成GetterSetter.gif)
复制预览
![复制预览](doc/img/复制预览.png)
注释生成演示
![插件演示](doc/img/插件演示.gif "插件演示")
