const fs = require('fs');
const path = require('path');
const yaml = require('js-yaml');

// 处理命令行参数
const args = process.argv.slice(2);
let customConfigPath = null;
let outputPath = path.join(__dirname, 'index.html');
let configDir = null;

// 解析命令行参数
for (let i = 0; i < args.length; i++) {
  if (args[i] === '--config' && i + 1 < args.length) {
    customConfigPath = args[i + 1];
    i++; // 跳过下一个参数，因为它已经被处理了
  } else if (args[i] === '--output' && i + 1 < args.length) {
    outputPath = args[i + 1];
    i++; // 跳过下一个参数
  } else if (args[i] === '--dir' && i + 1 < args.length) {
    configDir = args[i + 1];
    i++; // 跳过下一个参数
  }
}

// 读取模板
const template = fs.readFileSync(path.join(__dirname, 'template.html'), 'utf8');

// 处理目录参数 --dir
if (configDir) {
  // 确保路径是绝对路径
  if (!path.isAbsolute(configDir)) {
    configDir = path.join(__dirname, configDir);
  }
  
  console.log(`使用配置目录: ${configDir}`);
  
  try {
    // 尝试读取目录中的中文和英文配置文件
    const zhConfigPath = path.join(configDir, 'zh.yaml');
    const enConfigPath = path.join(configDir, 'en.yaml');
    
    // 检查文件是否存在
    if (!fs.existsSync(zhConfigPath)) {
      console.error(`错误: 中文配置文件不存在: ${zhConfigPath}`);
      process.exit(1);
    }
    
    if (!fs.existsSync(enConfigPath)) {
      console.error(`错误: 英文配置文件不存在: ${enConfigPath}`);
      process.exit(1);
    }
    
    // 读取配置文件内容
    const zhConfigContent = fs.readFileSync(zhConfigPath, 'utf8');
    const enConfigContent = fs.readFileSync(enConfigPath, 'utf8');
    
    // 解析YAML
    const zhConfig = yaml.load(zhConfigContent);
    const enConfig = yaml.load(enConfigContent);
    
    // 替换占位符，注入配置到模板中
    let result = template;
    
    // 替换中文配置占位符
    const zhPattern = /content = \{\}; \/\/ 初始化为空对象\s+\/\/ CONFIG_PLACEHOLDER_ZH/;
    if (template.match(zhPattern)) {
      result = result.replace(zhPattern, `content = ${JSON.stringify(zhConfig, null, 2)};`);
    } else {
      // 尝试另一种格式
      const altZhPattern = /content = \{\}; \/\/ CONFIG_PLACEHOLDER_ZH/;
      result = result.replace(altZhPattern, `content = ${JSON.stringify(zhConfig, null, 2)};`);
    }
    
    // 替换英文配置占位符
    const enPattern = /content = \{\}; \/\/ 初始化为空对象\s+\/\/ CONFIG_PLACEHOLDER_EN/;
    if (result.match(enPattern)) {
      result = result.replace(enPattern, `content = ${JSON.stringify(enConfig, null, 2)};`);
    } else {
      // 尝试另一种格式
      const altEnPattern = /content = \{\}; \/\/ CONFIG_PLACEHOLDER_EN/;
      result = result.replace(altEnPattern, `content = ${JSON.stringify(enConfig, null, 2)};`);
    }
    
    // 写入输出文件
    fs.writeFileSync(outputPath, result);
    
    console.log(`使用目录 ${configDir} 中的配置文件生成HTML成功!`);
    console.log(`输出文件: ${outputPath}`);
    return;
  } catch (error) {
    console.error(`处理配置目录时出错: ${error.message}`);
    process.exit(1);
  }
}

// 如果提供了自定义配置路径，则仅使用该配置
if (customConfigPath) {
  // 确保路径是绝对路径
  if (!path.isAbsolute(customConfigPath)) {
    customConfigPath = path.join(__dirname, customConfigPath);
  }
  
  console.log(`使用自定义配置文件: ${customConfigPath}`);
  
  try {
    // 读取自定义配置
    const customConfigContent = fs.readFileSync(customConfigPath, 'utf8');
    const customConfig = yaml.load(customConfigContent);
    
    // 确定是使用中文还是英文占位符
    // 根据文件名判断，通常 zh 在文件名中表示中文
    const isZhConfig = customConfigPath.includes('zh') || customConfigPath.includes('CN') || customConfigPath.includes('中文');
    
    // 替换占位符，注入配置到模板中
    let result = template;
    
    if (isZhConfig) {
      // 替换中文配置占位符
      const zhPattern = /content = \{\}; \/\/ 初始化为空对象\s+\/\/ CONFIG_PLACEHOLDER_ZH/;
      if (template.match(zhPattern)) {
        result = result.replace(zhPattern, `content = ${JSON.stringify(customConfig, null, 2)};`);
      } else {
        // 尝试另一种格式
        const altZhPattern = /content = \{\}; \/\/ CONFIG_PLACEHOLDER_ZH/;
        result = result.replace(altZhPattern, `content = ${JSON.stringify(customConfig, null, 2)};`);
      }
      
      // 清空英文配置
      const enPattern = /content = \{\}; \/\/ 初始化为空对象\s+\/\/ CONFIG_PLACEHOLDER_EN/;
      if (result.match(enPattern)) {
        result = result.replace(enPattern, `content = {}; // 初始化为空对象\n      // CONFIG_PLACEHOLDER_EN`);
      } else {
        // 尝试另一种格式
        const altEnPattern = /content = \{\}; \/\/ CONFIG_PLACEHOLDER_EN/;
        result = result.replace(altEnPattern, `content = {}; // CONFIG_PLACEHOLDER_EN`);
      }
    } else {
      // 替换英文配置占位符
      const enPattern = /content = \{\}; \/\/ 初始化为空对象\s+\/\/ CONFIG_PLACEHOLDER_EN/;
      if (result.match(enPattern)) {
        result = result.replace(enPattern, `content = ${JSON.stringify(customConfig, null, 2)};`);
      } else {
        // 尝试另一种格式
        const altEnPattern = /content = \{\}; \/\/ CONFIG_PLACEHOLDER_EN/;
        result = result.replace(altEnPattern, `content = ${JSON.stringify(customConfig, null, 2)};`);
      }
      
      // 清空中文配置
      const zhPattern = /content = \{\}; \/\/ 初始化为空对象\s+\/\/ CONFIG_PLACEHOLDER_ZH/;
      if (result.match(zhPattern)) {
        result = result.replace(zhPattern, `content = {}; // 初始化为空对象\n      // CONFIG_PLACEHOLDER_ZH`);
      } else {
        // 尝试另一种格式
        const altZhPattern = /content = \{\}; \/\/ CONFIG_PLACEHOLDER_ZH/;
        result = result.replace(altZhPattern, `content = {}; // CONFIG_PLACEHOLDER_ZH`);
      }
    }
    
    // 写入输出文件
    fs.writeFileSync(outputPath, result);
    
    console.log(`使用配置文件 ${customConfigPath} 生成HTML成功!`);
    console.log(`输出文件: ${outputPath}`);
  } catch (error) {
    console.error(`处理配置文件时出错: ${error.message}`);
    process.exit(1);
  }
} else if (!configDir) {
  // 使用默认的双语言配置
  
  // 读取中文配置
  const zhConfigPath = path.join(__dirname, 'config/sample/zh.yaml');
  const zhConfigContent = fs.readFileSync(zhConfigPath, 'utf8');
  const zhConfig = yaml.load(zhConfigContent);

  // 读取英文配置
  const enConfigPath = path.join(__dirname, 'config/sample/en.yaml');
  const enConfigContent = fs.readFileSync(enConfigPath, 'utf8');
  const enConfig = yaml.load(enConfigContent);

  // 替换占位符，注入配置到模板中
  let result = template;

  // 替换中文配置占位符
  const zhPattern = /content = \{\}; \/\/ 初始化为空对象\s+\/\/ CONFIG_PLACEHOLDER_ZH/;
  if (template.match(zhPattern)) {
    result = result.replace(zhPattern, `content = ${JSON.stringify(zhConfig, null, 2)};`);
  } else {
    // 尝试另一种格式
    const altZhPattern = /content = \{\}; \/\/ CONFIG_PLACEHOLDER_ZH/;
    result = result.replace(altZhPattern, `content = ${JSON.stringify(zhConfig, null, 2)};`);
  }

  // 替换英文配置占位符
  const enPattern = /content = \{\}; \/\/ 初始化为空对象\s+\/\/ CONFIG_PLACEHOLDER_EN/;
  if (result.match(enPattern)) {
    result = result.replace(enPattern, `content = ${JSON.stringify(enConfig, null, 2)};`);
  } else {
    // 尝试另一种格式
    const altEnPattern = /content = \{\}; \/\/ CONFIG_PLACEHOLDER_EN/;
    result = result.replace(altEnPattern, `content = ${JSON.stringify(enConfig, null, 2)};`);
  }

  // 写入输出文件
  fs.writeFileSync(outputPath, result);

  console.log('生成多语言支持的独立HTML文件成功!');
  console.log(`输出文件: ${outputPath}`);
}