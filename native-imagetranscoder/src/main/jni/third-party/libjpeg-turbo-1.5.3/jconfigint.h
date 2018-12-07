/* libjpeg-turbo build number */
#define BUILD "20180808"

/* Compiler's inline keyword */
/* #undef inline */

/* How to obtain function inlining. */
#define INLINE inline __attribute__((always_inline))

/* Define to the full name of this package. */
#define PACKAGE_NAME "libjpeg-turbo"

/* Version number of package */
#define VERSION "1.5.3"

/* The size of `size_t', as computed by sizeof. */
#ifdef __LP64__
#  define SIZEOF_SIZE_T 8
#else
#  define SIZEOF_SIZE_T 4
#endif
