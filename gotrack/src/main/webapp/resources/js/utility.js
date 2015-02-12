/**
 * @memberOf utility
 */
(function( utility, $, undefined ) {
   
   utility.isUndefined = function( variable ) {
      return ( typeof variable === 'undefined' );
   }
   
}( window.utility = window.utility || {}, jQuery ));