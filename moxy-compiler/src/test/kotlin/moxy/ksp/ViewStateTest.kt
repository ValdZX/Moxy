package moxy.ksp

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCompilerApi::class)
class ViewStateTest {
    @Test
    fun `Compile Generic test`() {
        val result = SourceFile.kotlin(
            "Generic.kt", """
        import moxy.InjectViewState
        import moxy.MvpPresenter
        import moxy.MvpView
        import moxy.viewstate.strategy.AddToEndSingleStrategy
        import moxy.viewstate.strategy.StateStrategyType
        
        interface GenericView<T> : MvpView {
            @StateStrategyType(AddToEndSingleStrategy::class)
            fun testEvent(count: Int, param: T)
        }
        
        @InjectViewState
        class GenericPresenter<T> : MvpPresenter<GenericView<T>?>()
    """
        ).compile()
        assertEquals(result.exitCode, KotlinCompilation.ExitCode.OK)
    }


    @Test
    fun `Compile Sample test`() {
        val result = SourceFile.kotlin(
            "DailyPictureView.kt", """
        import moxy.MvpView
        import moxy.MvpPresenter
        import moxy.viewstate.strategy.AddToEndSingleTagStrategy
        import moxy.viewstate.strategy.StateStrategyType
        import moxy.viewstate.strategy.alias.AddToEndSingle
        import moxy.viewstate.strategy.alias.OneExecution
        
        interface DailyPictureView : MvpView {
        
            @AddToEndSingle
            fun setTitle(text: String)
        
            @AddToEndSingle
            fun setDescription(text: String)
            
            @StateStrategyType(value = AddToEndSingleTagStrategy::class, tag = "show_hide_image")
            fun showImage(url: String)
        
            @StateStrategyType(value = AddToEndSingleTagStrategy::class, tag = "show_hide_image")
            fun showVideo()
        
            @StateStrategyType(value = AddToEndSingleTagStrategy::class, tag = "show_hide_image")
            fun hideImage()
        
            @StateStrategyType(value = AddToEndSingleTagStrategy::class, tag = "show_hide_copyright")
            fun showCopyright(text: String)
        
            @StateStrategyType(value = AddToEndSingleTagStrategy::class, tag = "show_hide_copyright")
            fun hideCopyright()
        
            @AddToEndSingle
            fun showProgress(isProgress: Boolean)
        
            @OneExecution
            fun openBrowser(url: String)
        
            @OneExecution
            fun showError(message: String)
        }

        class DailyPicturePresenter: MvpPresenter<DailyPictureView>()
    """
        ).compile()
        assertEquals(result.exitCode, KotlinCompilation.ExitCode.OK)
    }
    @Test
    fun `Ext Generic test`() {
        val result = SourceFile.kotlin(
            "Generic.kt", """
        import moxy.InjectViewState
        import moxy.MvpPresenter
        import moxy.MvpView
        import moxy.viewstate.strategy.AddToEndSingleStrategy
        import moxy.viewstate.strategy.StateStrategyType
        
        interface GenericView<T: Number> : MvpView {
            @StateStrategyType(AddToEndSingleStrategy::class)
            fun testEvent(count: Int, param: List<T>)
        }
        
        interface IntView : GenericView<Int>

        abstract class AbstractPresenter<V : CommonView> : MvpPresenter<V>()
        
        @InjectViewState
        open class StringPresenter<T, V: GenericView<T>> : AbstractPresenter<V>()
        
        @InjectViewState
        class ExtStringPresenter : StringPresenter<Short, IntView>()
        
        @InjectViewState
        class Ext2StringPresenter : StringPresenter<Short, GenericView<Short>>()
    """
        ).compile()
        assertEquals(result.exitCode, KotlinCompilation.ExitCode.OK)
    }
}

