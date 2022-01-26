using Microsoft.VisualStudio.DebuggerVisualizers;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Windows.Forms;

namespace M6e_Read_Write_Demo_Tool
{
    // TODO: aggiungere quanto segue alla definizione di SomeType per mostrare questo visualizzatore durante il debug di istanze di SomeType:
    // 
    //  [DebuggerVisualizer(typeof(Visualizer1))]
    //  [Serializable]
    //  public class SomeType
    //  {
    //   ...
    //  }
    // 
    /// <summary>
    /// Un visualizzatore per SomeType.  
    /// </summary>
    public class Visualizer1 : DialogDebuggerVisualizer
    {
        protected override void Show(IDialogVisualizerService windowService, IVisualizerObjectProvider objectProvider)
        {
            if (windowService == null)
                throw new ArgumentNullException("windowService");
            if (objectProvider == null)
                throw new ArgumentNullException("objectProvider");

            // TODO: ottenere l'oggetto per cui mostrare un visualizzatore.
            //       Eseguire il cast del risultato di objectProvider.GetObject() 
            //       al tipo dell'oggetto visualizzato.
            object data = (object)objectProvider.GetObject();

            // TODO: mostrare la visualizzazione dell'oggetto.
            //       Sostituire displayForm con il form o il controllo personalizzato.
            using (Form displayForm = new Form())
            {
                displayForm.Text = data.ToString();
                windowService.ShowDialog(displayForm);
            }
        }

        // TODO: aggiungere quanto segue al codice per testare il visualizzatore:
        // 
        //    Visualizer1.TestShowVisualizer(new SomeType());
        // 
        /// <summary>
        /// Effettua un test del visualizzatore eseguendo l'hosting all'esterno del debugger.
        /// </summary>
        /// <param name="objectToVisualize">Oggetto da mostrare nel visualizzatore.</param>
        public static void TestShowVisualizer(object objectToVisualize)
        {
            VisualizerDevelopmentHost visualizerHost = new VisualizerDevelopmentHost(objectToVisualize, typeof(Visualizer1));
            visualizerHost.ShowVisualizer();
        }
    }
}
