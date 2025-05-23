package com.googlecode.aviator;

import com.ql.util.express.DefaultContext;
import com.ql.util.express.ExpressLoader;
import com.ql.util.express.ExpressRunner;
import com.ql.util.express.InstructionSet;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.beetl.core.Configuration;
import org.beetl.core.GroupTemplate;
import org.beetl.core.resource.StringTemplateResourceLoader;
import org.junit.Assert;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelCompiler;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * Benchmark                                        Mode  Cnt      Score     Error   Units
 * PerfBenchmark.testArith                        thrpt    5  103458.853 ± 2446.561  ops/ms
 * PerfBenchmark.testArithByAviator               thrpt    5    2490.415 ±   57.684  ops/ms
 * PerfBenchmark.testArithByAviatorInterpretMode  thrpt    5    1686.681 ±   14.926  ops/ms
 * PerfBenchmark.testArithByBeetl                 thrpt    5    3904.690 ±  146.051  ops/ms
 * PerfBenchmark.testArithByMVEL                  thrpt    5    5172.483 ±   39.956  ops/ms
 * PerfBenchmark.testArithByMVELStrongType        thrpt    5    7277.194 ±  109.768  ops/ms
 * PerfBenchmark.testArithByScript                thrpt    5    7727.575 ±  283.199  ops/ms
 * PerfBenchmark.testArithBySpel                  thrpt    5   63783.590 ±  726.086  ops/ms
 * PerfBenchmark.testArithBySpelInterpretMode     thrpt    5     836.949 ±   14.800  ops/ms
 * PerfBenchmark.testArithByQL                    thrpt    5     441.561 ±   26.886  ops/ms
 * 
 * PerfBenchmark.testCond                          thrpt    5  65466.293 ± 622.039  ops/ms
 * PerfBenchmark.testCondByAviator                 thrpt    5   1216.847 ±  12.346  ops/ms
 * PerfBenchmark.testCondByAviatorInterpretMode    thrpt    5    584.566 ±   5.683  ops/ms
 * PerfBenchmark.testCondByBeetl                   thrpt    5   2826.764 ±  26.029  ops/ms
 * PerfBenchmark.testCondByScript                  thrpt    5   6044.490 ±  36.924  ops/ms
 * PerfBenchmark.testCondByQL                      thrpt    5    420.605 ±  38.177  ops/ms
 * PerfBenchmark.testObject                        thrpt    5   6635.216 ± 125.474  ops/ms
 * PerfBenchmark.testObjectByAviator               thrpt    5    843.353 ±  20.206  ops/ms
 * PerfBenchmark.testObjectByAviatorInterpretMode  thrpt    5    535.820 ±  10.644  ops/ms
 * PerfBenchmark.testObjectByBeetl                 thrpt    5   1844.948 ± 116.365  ops/ms
 * PerfBenchmark.testObjectByScript                thrpt    5   4228.086 ± 300.960  ops/ms
 * PerfBenchmark.testObjectByQL                    thrpt    5    236.309 ±  69.323  ops/ms
 *
 * 2.6 GHz 六核Intel Core i7
 * */
@Warmup(iterations = 3, time = 10, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 10, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class PerfBenchmark {

  public static class Data {
    public String ikey;
    public Integer ivalue;

    public Data(final String ikey, final int ivalue) {
      setIkey(ikey);
      this.ivalue = ivalue;
    }

    public Integer getIvalue() {
      return this.ivalue;
    }

    public void setIvalue(final Integer ivalue) {
      this.ivalue = ivalue;
    }

    public String getIkey() {
      return this.ikey;
    }

    public void setIkey(final String ikey) {
      this.ikey = ikey;
    }
  }


  private final Map<String, Object> paras = new HashMap<>();

  private final EvaluationContext nh = new StandardEvaluationContext();

  private GroupTemplate gt;

  private Invocable arithInv;
  private Invocable objectInv;
  private Invocable condInv;

  private Expression arithExp;
  private Expression objectExp;
  private Expression condExp;

  private Expression arithExpInterpret;
  private Expression objectExpInterpret;
  private Expression condExpInterpret;

  private org.springframework.expression.Expression arithExpSpelInterpret;
  private org.springframework.expression.Expression arithExpSpel;

  private Object arithExpMVEL;
  private Object objectExpMVEL;
  private Object condExpMVEL;

  private Object arithExpMVELStrongType;
  private Object objectExpMVELStrongType;
  private Object condExpMVELStrongType;

  private ExpressRunner runner;
  private DefaultContext<String, Object> dc;
  private InstructionSet arithExpQL;
  private InstructionSet objectExpQL;
  private InstructionSet condExpQL;

  @Setup
  public void init() {
    this.paras.put("A", new Data("false", 23342423));
    this.paras.put("B", new Data("false", 435454));
    this.paras.put("C", new Data("true", 121243));
    this.paras.put("D", new Data("false", 23));

    initScript();
    initBeetl();
    initAviator();
    initAviatorInterpreterMode();
    initSpel();
    initMVEL();
    initQL();
  }

  @Benchmark
  public Object testArith() throws Exception {
    return
        (((Data) this.paras.get("A")).getIvalue() + ((Data) this.paras.get("B")).getIvalue()
            - ((Data) this.paras.get("C")).getIvalue()) * ((Data) this.paras.get("D")).getIvalue();
  }

  @Benchmark
  public Object testObject() throws Exception {
    Map<String, Object> result = new HashMap<>(4);
    result.put("f1", ((Data) this.paras.get("A")).getIvalue());
    result.put("f2",
        ((Data) this.paras.get("A")).getIvalue() + ((Data) this.paras.get("B")).getIvalue());
    result.put("f3", ((Data) this.paras.get("C")).getIvalue());
    result
        .put("f4",
            (((Data) this.paras.get("A")).getIvalue() + ((Data) this.paras.get("B")).getIvalue()
                - ((Data) this.paras.get("C")).getIvalue())
                * ((Data) this.paras.get("D")).getIvalue());
    return result;
  }

  @Benchmark
  public Object testCond() throws Exception {
    Object result = 0;
    if (((Data) this.paras.get("A")).getIkey().equals("true")) {
      result = ((Data) this.paras.get("A")).getIvalue();
    } else if (((Data) this.paras.get("B")).getIkey().equals("true")) {
      result = ((Data) this.paras.get("B")).getIvalue();
    } else if (((Data) this.paras.get("C")).getIkey().equals("true")) {
      result = ((Data) this.paras.get("C")).getIvalue();
    } else if (((Data) this.paras.get("D")).getIkey().equals("true")) {
      result = ((Data) this.paras.get("D")).getIvalue();
    }
    return result;
  }

  @Benchmark
  public void testArithByScript() throws Exception {
    Object result = this.arithInv.invokeFunction("testArith", this.paras);
  }

  @Benchmark
  public void testObjectByScript() throws Exception {
    Object result = this.objectInv.invokeFunction("testObject", this.paras);
  }

  @Benchmark
  public void testCondByScript() throws Exception {
    Object result = this.condInv.invokeFunction("testCond", this.paras);
  }

  @Benchmark
  public void testArithByBeetl() {
    Map result = this.gt.runScript("return (A.ivalue+B.ivalue-C.ivalue)*D.ivalue;", this.paras);
  }

  @Benchmark
  public void testObjectByBeetl() {
    Map result = this.gt.runScript(
        "var object = {f1: A.ivalue, f2: A.ivalue+B.ivalue, f3: C.ivalue, f4: (A.ivalue+B.ivalue-C.ivalue)*D.ivalue}; ",
        this.paras);
  }

  @Benchmark
  public void testCondByBeetl() {
    Map result = this.gt.runScript(
        "if(A.ikey=='true'){return A.ivalue;}else if(B.ikey=='true'){return B.ivalue;}else if(C.ikey=='true'){return C.ivalue;}else if(D.ikey=='true'){return D.ivalue;}else{return 0;}",
        this.paras);
  }

  @Benchmark
  public void testArithByAviatorInterpretMode() {
    Object result = this.arithExpInterpret.execute(this.paras);
  }

  @Benchmark
  public void testObjectByAviatorInterpretMode() {
    Object result = this.objectExpInterpret.execute(this.paras);
  }

  @Benchmark
  public void testCondByAviatorInterpretMode() {
    Object result = this.condExpInterpret.execute(this.paras);
  }


  @Benchmark
  public void testArithByAviator() {
    Object result = this.arithExp.execute(this.paras);
  }

  @Benchmark
  public void testObjectByAviator() {
    Object result = this.objectExp.execute(this.paras);
  }

  @Benchmark
  public void testCondByAviator() {
    Object result = this.condExp.execute(this.paras);
  }

  @Benchmark
  public void testArithBySpelInterpretMode() {
    Object result = this.arithExpSpelInterpret.getValue(nh);
  }

  @Benchmark
  public void testArithBySpel() {
    Object result = this.arithExpSpel.getValue(nh);
  }

  @Benchmark
  public void testArithByMVEL() {
    Object result = MVEL.executeExpression(this.arithExpMVEL, this.paras);
  }

  @Benchmark
  public void testObjectByMVEL() {
    Object result = MVEL.executeExpression(this.objectExpMVEL, this.paras);
  }

  @Benchmark
  public void testCondByMVEL() {
    Object result = MVEL.executeExpression(this.condExpMVEL, this.paras);
  }

  @Benchmark
  public void testArithByMVELStrongType() {
    Object result = MVEL.executeExpression(this.arithExpMVELStrongType, this.paras);
  }

  @Benchmark
  public void testObjectByMVELStrongType() {
    Object result = MVEL.executeExpression(this.objectExpMVELStrongType, this.paras);
  }

  @Benchmark
  public void testCondByMVELStrongType() {
    Object result = MVEL.executeExpression(this.condExpMVELStrongType, this.paras);
  }

  @Benchmark
  public void testArithByQL() throws Exception {
    Object result = runner.execute(arithExpQL, dc, null, false, false);
  }

  @Benchmark
  public void testObjectByQL() throws Exception {
    Object result = runner.execute(objectExpQL, dc, null, false, false);
  }

  @Benchmark
  public void testCondByQL() throws Exception {
    Object result = runner.execute(condExpQL, dc, null, false, false);
  }


  private void initScript() {
    ScriptEngineManager manager = new ScriptEngineManager();
    try {
      ScriptEngine engine = manager.getEngineByName("js");
      engine.eval(
          "function testArith(paras){return (paras.A.ivalue+paras.B.ivalue-paras.C.ivalue)*paras.D.ivalue;}");
      this.arithInv = (Invocable) engine;

      engine = manager.getEngineByName("js");
      engine.eval(
          "function testObject(paras){var object={f1: paras.A.ivalue, f2: paras.A.ivalue+paras.B.ivalue, f3: paras.C.ivalue, f4: (paras.A.ivalue+paras.B.ivalue-paras.C.ivalue)*paras.D.ivalue}; return object;}");
      this.objectInv = (Invocable) engine;

      engine = manager.getEngineByName("js");
      engine.eval(
          "function testCond(paras){if(paras.A.ikey=='true'){return paras.A.ivalue;}else if(paras.B.ikey=='true'){return paras.B.ivalue;}else if(paras.C.ikey=='true'){return paras.C.ivalue;}else if(paras.D.ikey=='true'){return paras.D.ivalue;}else{return 0;}}");
      this.condInv = (Invocable) engine;
    } catch (ScriptException e) {
      e.printStackTrace();
    }
    System.out.println("JS准备工作就绪！");
  }

  private void initBeetl() {
    try {
      Configuration cfg = Configuration.defaultConfiguration();
      this.gt = new GroupTemplate(new StringTemplateResourceLoader(), cfg);
    } catch (IOException e) {
      throw new RuntimeException("初始化Beetl资源加载器失败", e);
    }
    System.out.println("Beetl准备工作就绪！");
  }

  private void initAviator() {
    AviatorEvaluatorInstance instance = AviatorEvaluator.newInstance(EvalMode.ASM);
    this.arithExp = instance.compile("(A.ivalue+B.ivalue-C.ivalue)*D.ivalue");
    this.objectExp = instance.compile(
        "let object=seq.map('f1', A.ivalue, 'f2', A.ivalue+B.ivalue, 'f3', C.ivalue, 'f4', (A.ivalue+B.ivalue-C.ivalue)*D.ivalue); return object;");
    this.condExp = instance.compile(
        "if(A.ikey=='true'){return A.ivalue;}elsif(B.ikey=='true'){return B.ivalue;}elsif(C.ikey=='true'){return C.ivalue;}elsif(D.ikey=='true'){return D.ivalue;}else{return 0;}");
    System.out.println("Aviator ASM 模式准备工作就绪！");
  }

  private void initAviatorInterpreterMode() {
    AviatorEvaluatorInstance instance = AviatorEvaluator.newInstance(EvalMode.INTERPRETER);
    this.arithExpInterpret = instance.compile("(A.ivalue+B.ivalue-C.ivalue)*D.ivalue");
    this.objectExpInterpret = instance.compile(
        "let object=seq.map('f1', A.ivalue, 'f2', A.ivalue+B.ivalue, 'f3', C.ivalue, 'f4', (A.ivalue+B.ivalue-C.ivalue)*D.ivalue); return object;");
    this.condExpInterpret = instance.compile(
        "if(A.ikey=='true'){return A.ivalue;}elsif(B.ikey=='true'){return B.ivalue;}elsif(C.ikey=='true'){return C.ivalue;}elsif(D.ikey=='true'){return D.ivalue;}else{return 0;}");
    System.out.println("Aviator 解释器模式准备工作就绪！");
  }

  private void initSpel() {
    for (Entry<String, Object> entry : this.paras.entrySet()) {
      nh.setVariable(entry.getKey(), entry.getValue());
    }

    ExpressionParser parser = new SpelExpressionParser();
    arithExpSpelInterpret = parser.parseExpression("(#A.ivalue+#B.ivalue-#C.ivalue)*#D.ivalue");
    System.out.println("Spel 解释器模式准备工作就绪！");

    arithExpSpel = parser.parseExpression("(#A.ivalue+#B.ivalue-#C.ivalue)*#D.ivalue");
    this.arithExpSpel.getValue(nh);
    Assert.assertTrue(SpelCompiler.compile(arithExpSpel));
    System.out.println("Spel ASM 模式准备工作就绪！");
  }

  //-Dmvel2.advanced_debugging=true -Dmvel2.disable.jit=true
  // asm accessor 没有比 reflect accessor 快多少, 快10%左右
  private void initMVEL() {
    arithExpMVEL = MVEL.compileExpression("(A.ivalue+B.ivalue-C.ivalue)*D.ivalue");
    objectExpMVEL = MVEL.compileExpression("object = ['f1': A.ivalue, 'f2': A.ivalue+B.ivalue, 'f3': C.ivalue, 'f4': (A.ivalue+B.ivalue-C.ivalue)*D.ivalue]; return object;");
    condExpMVEL = MVEL.compileExpression("if(A.ikey=='true'){return A.ivalue;}else if(B.ikey=='true'){return B.ivalue;}else if(C.ikey=='true'){return C.ivalue;}else if(D.ikey=='true'){return D.ivalue;}else{return 0;}");
    System.out.println("MVEL 准备工作就绪！");

    ParserContext parserContext = new ParserContext();
    parserContext.setStrongTyping(true);
    parserContext.setStrictTypeEnforcement(true);
    parserContext.addInput("A", Data.class);
    parserContext.addInput("B", Data.class);
    parserContext.addInput("C", Data.class);
    parserContext.addInput("D", Data.class);
    arithExpMVELStrongType = MVEL.compileExpression("(A.ivalue+B.ivalue-C.ivalue)*D.ivalue", parserContext);
    objectExpMVELStrongType = MVEL.compileExpression("object = ['f1': A.ivalue, 'f2': A.ivalue+B.ivalue, 'f3': C.ivalue, 'f4': (A.ivalue+B.ivalue-C.ivalue)*D.ivalue]; return object;", parserContext);
    condExpMVELStrongType = MVEL.compileExpression("if(A.ikey=='true'){return A.ivalue;}else if(B.ikey=='true'){return B.ivalue;}else if(C.ikey=='true'){return C.ivalue;}else if(D.ikey=='true'){return D.ivalue;}else{return 0;}", parserContext);
    System.out.println("MVEL StrongType 模式准备工作就绪！");
  }

  private void initQL() {
    runner = new ExpressRunner();
    ExpressLoader loader = new ExpressLoader(runner);

    dc = new DefaultContext<>();
    dc.putAll(this.paras);

    try {
      arithExpQL = loader.parseInstructionSet("arithExpQL", "(A.ivalue+B.ivalue-C.ivalue)*D.ivalue");
      objectExpQL = loader.parseInstructionSet("objectExpQL",
              "abc = NewMap('f1': A.ivalue, 'f2': A.ivalue+B.ivalue, 'f3': C.ivalue, 'f4': (A.ivalue+B.ivalue-C.ivalue)*D.ivalue); return abc;");
      condExpQL = loader.parseInstructionSet("condExpQL",
              "if(A.ikey=='true'){return A.ivalue;}else if(B.ikey=='true'){return B.ivalue;}else if(C.ikey=='true'){return C.ivalue;}else if(D.ikey=='true'){return D.ivalue;}else{return 0;}");
      System.out.println("QL Expression 模式准备工作就绪！");
    } catch (Exception e) {
       e.printStackTrace();
    }
  }

  public static void main(final String[] args) throws Exception {
    Options opt = new OptionsBuilder().include(PerfBenchmark.class.getSimpleName()).build();
    new Runner(opt).run();
  }

  public static void profile() throws Exception {
    PerfBenchmark benchmark = new PerfBenchmark();
    benchmark.init();
    for (int i = 0; i < 999999900; i++) {
      benchmark.testArithByQL();
    }
  }
  
  public static void testTimeoutQL() throws Exception {
    ExpressRunner runner = new ExpressRunner();
    Object ret = runner.execute("int i = 10; i = i + 100; java.lang.Thread.sleep(10000); return i;", null, null, false, false, 1000);
    System.out.println(ret);
  }



}
