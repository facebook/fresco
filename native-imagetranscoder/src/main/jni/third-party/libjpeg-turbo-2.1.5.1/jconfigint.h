/* libjpeg-turbo build number */
#define BUILD "20240418"

/* Compiler's inline keyword */
/* #undef inline */

/* How to obtain function inlining. */
#define INLINE inline __attribute__((always_inline))

/* How to obtain thread-local storage */
#define THREAD_LOCAL  __thread

/* Define to the full name of this package. */
#define PACKAGE_NAME "libjpeg-turbo"

/* Version number of package */
#define VERSION "2.1.5.1"

/* The size of `size_t', as computed by sizeof. */
#ifdef __LP64__
#  define SIZEOF_SIZE_T 8
#else
#  define SIZEOF_SIZE_T 4
#endif

#if defined(__has_attribute)
#if __has_attribute(fallthrough)
#define FALLTHROUGH __attribute__((fallthrough));
#else
#define FALLTHROUGH
#endif
#else
#define FALLTHROUGH
#endif
