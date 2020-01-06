/*
 * Copyright (c) 2018 datagear.tech. All Rights Reserved.
 */

/**
 * 
 */
package org.datagear.analysis.support.html;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.datagear.analysis.support.ChartWidgetSource;
import org.datagear.analysis.support.DashboardWidgetResManager;
import org.datagear.util.StringUtil;

import freemarker.core.Environment;
import freemarker.ext.util.WrapperTemplateModel;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;

/**
 * 使用Freemarker作为模板的{@linkplain HtmlTplDashboardWidget}渲染器。
 * <p>
 * 此类可渲染由{@linkplain DashboardWidgetResManager}管理模板的{@linkplain HtmlTplDashboardWidget}，
 * 其中{@linkplain HtmlTplDashboardWidget#getTemplate()}应该是可以通过{@linkplain DashboardWidgetResManager#getFile(String, String)}找到的模板文件名。
 * </p>
 * <p>
 * 此类需要手动调用{@linkplain #init()}方法进行初始化。
 * </p>
 * <p>
 * 支持的模板格式如下：
 * </p>
 * <code>
 * <pre>
 * ...
 * &lt;@import /&gt;
 * ...
 * &lt;@theme /&gt;
 * ...
 * &lt;@dashboard var="..." listener="..."&gt;
 *   ...
 *   <@chart widget="..." var="..." elementId="..." /&gt;
 *   ...
 *   <@chart widget="..." var="..." elementId="..." /&gt;
 *   ...
 * &lt;/@dashboard&gt;
 * </pre>
 * </code>
 * <p>
 * &lt;@import /&gt;：引入内置JS、CSS等HTML资源。
 * </p>
 * <p>
 * &lt;@theme /&gt;：引入内置CSS主题样式。
 * </p>
 * <p>
 * &lt;@dashboard&gt;：定义看板，“var”自定义看板JS变量名，可不填；“listener”自定义看板JS监听器，可不填。
 * </p>
 * <p>
 * &lt;@chart
 * /&gt;：定义图表，“widget”为{@linkplain HtmlChartWidget#getId()}，必填；“var”自定义图表JS变量名，可不填；“elementId”自定义图表HTML元素ID，可不填。
 * </p>
 * 
 * @author datagear@163.com
 *
 * @param <T>
 */
public class HtmlTplDashboardWidgetFmkRenderer<T extends HtmlRenderContext> extends HtmlTplDashboardWidgetRenderer<T>
{
	public static final String DIRECTIVE_IMPORT = "import";

	public static final String DIRECTIVE_THEME = "theme";

	public static final String DIRECTIVE_DASHBOARD = "dashboard";

	public static final String DIRECTIVE_CHART = "chart";

	protected static final String KEY_HTML_DASHBOARD_RENDER_DATA_MODEL = HtmlDashboardRenderDataModel.class
			.getSimpleName();

	private String defaultTemplateEncoding = "UTF-8";

	private boolean ignoreDashboardStyleBorderWidth = true;

	private Configuration _configuration;

	public HtmlTplDashboardWidgetFmkRenderer()
	{
		super();
	}

	public HtmlTplDashboardWidgetFmkRenderer(DashboardWidgetResManager dashboardWidgetResManager,
			ChartWidgetSource chartWidgetSource)
	{
		super(dashboardWidgetResManager, chartWidgetSource);
	}

	public String getDefaultTemplateEncoding()
	{
		return defaultTemplateEncoding;
	}

	public void setDefaultTemplateEncoding(String defaultTemplateEncoding)
	{
		this.defaultTemplateEncoding = defaultTemplateEncoding;
	}

	public boolean isIgnoreDashboardStyleBorderWidth()
	{
		return ignoreDashboardStyleBorderWidth;
	}

	public void setIgnoreDashboardStyleBorderWidth(boolean ignoreDashboardStyleBorderWidth)
	{
		this.ignoreDashboardStyleBorderWidth = ignoreDashboardStyleBorderWidth;
	}

	/**
	 * 初始化。
	 * 
	 * @throws IOException
	 */
	public void init() throws IOException
	{
		Configuration cfg = new Configuration(Configuration.VERSION_2_3_28);
		cfg.setDirectoryForTemplateLoading(getDashboardWidgetResManager().getRootDirectory());
		cfg.setDefaultEncoding(this.defaultTemplateEncoding);
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
		cfg.setLogTemplateExceptions(false);
		cfg.setWrapUncheckedExceptions(true);

		cfg.setSharedVariable(DIRECTIVE_IMPORT, new ImportTemplateDirectiveModel());
		cfg.setSharedVariable(DIRECTIVE_THEME, new ThemeTemplateDirectiveModel());
		cfg.setSharedVariable(DIRECTIVE_DASHBOARD, new DashboardTemplateDirectiveModel());
		cfg.setSharedVariable(DIRECTIVE_CHART, new ChartTemplateDirectiveModel());

		setConfiguration(cfg);
	}

	@Override
	protected void renderHtmlDashboard(T renderContext, HtmlDashboard dashboard) throws Throwable
	{
		HtmlDashboardRenderDataModel dataModel = new HtmlDashboardRenderDataModel(dashboard,
				renderContext.getWebContext().getContextPath());

		Template template = getTemplate((HtmlTplDashboardWidget<?>) dashboard.getWidget());

		template.process(buildHtmlDashboardRenderDataModel(dataModel), renderContext.getWriter());
	}

	/**
	 * 获取{@linkplain HtmlTplDashboardWidget#getId()}的指定模板对象。
	 * 
	 * @param dashboardWidget
	 * @return
	 * @throws Exception
	 */
	protected Template getTemplate(HtmlTplDashboardWidget<?> dashboardWidget) throws Exception
	{
		String path = getDashboardWidgetResManager().getRelativePath(dashboardWidget.getId(),
				dashboardWidget.getTemplate());

		return getConfiguration().getTemplate(path);
	}

	protected Configuration getConfiguration()
	{
		return _configuration;
	}

	protected void setConfiguration(Configuration _configuration)
	{
		this._configuration = _configuration;
	}

	protected Object buildHtmlDashboardRenderDataModel(HtmlDashboardRenderDataModel dataModel)
	{
		Map<String, Object> map = new HashMap<String, Object>();

		map.put(KEY_HTML_DASHBOARD_RENDER_DATA_MODEL, dataModel);

		return map;
	}

	protected HtmlDashboardRenderDataModel getHtmlDashboardRenderDataModel(Environment env)
			throws TemplateModelException
	{
		TemplateHashModel templateHashModel = env.getDataModel();
		HtmlDashboardRenderDataModel dataModel = (HtmlDashboardRenderDataModel) templateHashModel
				.get(KEY_HTML_DASHBOARD_RENDER_DATA_MODEL);

		return dataModel;
	}

	/**
	 * HTML看板渲染数据模型。
	 * 
	 * @author datagear@163.com
	 *
	 */
	protected static class HtmlDashboardRenderDataModel implements WrapperTemplateModel
	{
		private HtmlDashboard htmlDashboard;

		private String contextPath = "";

		public HtmlDashboardRenderDataModel()
		{
			super();
		}

		public HtmlDashboardRenderDataModel(HtmlDashboard htmlDashboard, String contextPath)
		{
			super();
			this.htmlDashboard = htmlDashboard;
			this.contextPath = contextPath;
		}

		public HtmlDashboard getHtmlDashboard()
		{
			return htmlDashboard;
		}

		public void setHtmlDashboard(HtmlDashboard htmlDashboard)
		{
			this.htmlDashboard = htmlDashboard;
		}

		public String getContextPath()
		{
			return contextPath;
		}

		public void setContextPath(String contextPath)
		{
			this.contextPath = contextPath;
		}

		@Override
		public Object getWrappedObject()
		{
			return this.htmlDashboard;
		}
	}

	protected abstract class AbstractTemplateDirectiveModel implements TemplateDirectiveModel
	{
		public AbstractTemplateDirectiveModel()
		{
			super();
		}

		/**
		 * 获取字符串参数值。
		 * 
		 * @param params
		 * @param key
		 * @return
		 * @throws TemplateModelException
		 */
		protected String getStringParamValue(Map<?, ?> params, String key) throws TemplateModelException
		{
			Object value = params.get(key);

			if (value == null)
				return null;
			else if (value instanceof String)
				return (String) value;
			else if (value instanceof TemplateScalarModel)
				return ((TemplateScalarModel) value).getAsString();
			else
				throw new TemplateModelException(
						"Can not get string from [" + value.getClass().getName() + "] instance");
		}
	}

	/**
	 * “@import”指令。
	 * 
	 * @author datagear@163.com
	 *
	 */
	protected class ImportTemplateDirectiveModel extends AbstractTemplateDirectiveModel
	{
		public ImportTemplateDirectiveModel()
		{
			super();
		}

		@SuppressWarnings("rawtypes")
		@Override
		public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
				throws TemplateException, IOException
		{
			HtmlDashboardRenderDataModel dataModel = getHtmlDashboardRenderDataModel(env);
			HtmlDashboard dashboard = dataModel.getHtmlDashboard();
			HtmlRenderContext renderContext = dashboard.getRenderContext();

			writeDashboardImport(renderContext, dashboard, "");
		}
	}

	/**
	 * “@theme”指令。
	 * 
	 * @author datagear@163.com
	 *
	 */
	protected class ThemeTemplateDirectiveModel extends AbstractTemplateDirectiveModel
	{
		public ThemeTemplateDirectiveModel()
		{
			super();
		}

		@SuppressWarnings("rawtypes")
		@Override
		public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
				throws TemplateException, IOException
		{
			HtmlDashboardRenderDataModel dataModel = getHtmlDashboardRenderDataModel(env);
			HtmlDashboard dashboard = dataModel.getHtmlDashboard();
			HtmlRenderContext renderContext = dashboard.getRenderContext();

			Writer out = env.getOut();

			writeDashboardThemeStyle(renderContext, dashboard, out);
		}
	}

	/**
	 * “@dashboard”指令。
	 * 
	 * @author datagear@163.com
	 *
	 */
	protected class DashboardTemplateDirectiveModel extends AbstractTemplateDirectiveModel
	{
		public DashboardTemplateDirectiveModel()
		{
			super();
		}

		@SuppressWarnings("rawtypes")
		@Override
		public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
				throws TemplateException, IOException
		{
			String dashboardVar = getStringParamValue(params, "var");
			String listenerVar = getStringParamValue(params, "listener");

			HtmlDashboardRenderDataModel dataModel = getHtmlDashboardRenderDataModel(env);
			HtmlDashboard dashboard = dataModel.getHtmlDashboard();
			HtmlRenderContext renderContext = dashboard.getRenderContext();
			int nextSequence = -1;

			if (StringUtil.isEmpty(dashboardVar))
			{
				nextSequence = HtmlRenderAttributes.getNextSequenceIfNot(renderContext, nextSequence);
				dashboardVar = HtmlRenderAttributes.generateDashboardVarName(nextSequence);
			}

			dashboard.setVarName(dashboardVar);

			Writer out = env.getOut();

			writeScriptStartTag(out);
			writeNewLine(out);

			writeHtmlDashboardJSVar(out, dashboard, true);

			writeScriptEndTag(out);
			writeNewLine(out);

			HtmlRenderAttributes.setChartRenderContextVarName(renderContext, dashboardVar + ".renderContext");

			if (body != null)
				body.render(out);

			writeScriptStartTag(out);
			writeNewLine(out);

			String tmpRenderContextVar = HtmlRenderAttributes.generateRenderContextVarName(nextSequence);

			// 移除内部设置的属性
			HtmlRenderAttributes.removeChartRenderContextVarName(renderContext);
			HtmlRenderAttributes.removeChartNotRenderScriptTag(renderContext);
			HtmlRenderAttributes.removeChartScriptNotInvokeRender(renderContext);
			HtmlRenderAttributes.removeChartVarName(renderContext);
			HtmlRenderAttributes.removeChartElementId(renderContext);
			renderContext.removeAttribute(RENDER_ATTR_NAME_FOR_NOT_FOUND_SCRIPT);

			writeHtmlDashboardJSInit(out, dashboard, tmpRenderContextVar, listenerVar);

			writeScriptEndTag(out);
			writeNewLine(out);
		}
	}

	/**
	 * “@chart”指令。
	 * 
	 * @author datagear@163.com
	 *
	 */
	protected class ChartTemplateDirectiveModel extends AbstractTemplateDirectiveModel
	{
		public ChartTemplateDirectiveModel()
		{
			super();
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
				throws TemplateException, IOException
		{
			String widget = getStringParamValue(params, "widget");
			String var = getStringParamValue(params, "var");
			String elementId = getStringParamValue(params, "elementId");

			HtmlDashboardRenderDataModel dataModel = getHtmlDashboardRenderDataModel(env);
			HtmlDashboard htmlDashboard = dataModel.getHtmlDashboard();
			HtmlRenderContext renderContext = htmlDashboard.getRenderContext();
			int nextSequence = -1;

			HtmlChartWidget<HtmlRenderContext> chartWidget = getHtmlChartWidgetForRender(renderContext, widget);

			if (StringUtil.isEmpty(var))
			{
				nextSequence = HtmlRenderAttributes.getNextSequenceIfNot(renderContext, nextSequence);
				var = HtmlRenderAttributes.generateChartVarName(nextSequence);
			}

			if (StringUtil.isEmpty(elementId))
			{
				nextSequence = HtmlRenderAttributes.getNextSequenceIfNot(renderContext, nextSequence);
				elementId = HtmlRenderAttributes.generateChartElementId(nextSequence);
			}

			HtmlRenderAttributes.setChartNotRenderScriptTag(renderContext, false);
			HtmlRenderAttributes.setChartScriptNotInvokeRender(renderContext, true);
			HtmlRenderAttributes.setChartVarName(renderContext, var);
			HtmlRenderAttributes.setChartElementId(renderContext, elementId);

			HtmlChart chart = chartWidget.render(renderContext);

			List<HtmlChart> charts = (List<HtmlChart>) htmlDashboard.getCharts();
			if (charts == null)
			{
				charts = new ArrayList<HtmlChart>();
				htmlDashboard.setCharts(charts);
			}

			charts.add(chart);
		}
	}
}