/**
 * @memberOf utility
 */
(function( utility, $, undefined ) {
   
   utility.isUndefined = function( variable ) {
      return ( typeof variable === 'undefined' );
   }
   
   utility.roundHalf = function(num) {
      num = Math.round(num*2)/2;
      return num;
  }
   
}( window.utility = window.utility || {}, jQuery ));