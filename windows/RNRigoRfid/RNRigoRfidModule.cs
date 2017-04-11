using ReactNative.Bridge;
using System;
using System.Collections.Generic;
using Windows.ApplicationModel.Core;
using Windows.UI.Core;

namespace Com.Rigoiot.RNRigoRfid
{
    /// <summary>
    /// A module that allows JS to share data.
    /// </summary>
    class RNRigoRfidModule : NativeModuleBase
    {
        /// <summary>
        /// Instantiates the <see cref="RNRigoRfidModule"/>.
        /// </summary>
        internal RNRigoRfidModule()
        {

        }

        /// <summary>
        /// The name of the native module.
        /// </summary>
        public override string Name
        {
            get
            {
                return "RNRigoRfid";
            }
        }
    }
}
