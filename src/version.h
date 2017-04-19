/*
 * Define the version number, name, etc.
 * The patchlevel is in included_patches[], in version.c.
 */

#define VIM_VERSION_MAJOR                7
#define VIM_VERSION_MAJOR_STR           "7"
#define VIM_VERSION_MINOR                4
#define VIM_VERSION_MINOR_STR           "4"
#define VIM_VERSION_100     (VIM_VERSION_MAJOR * 100 + VIM_VERSION_MINOR)

/*
 * VIM_VERSION_NODOT is used for the runtime directory name.
 * VIM_VERSION_SHORT is copied into the swap file (max. length is 6 chars).
 * VIM_VERSION_LONG is used for the ":version" command and "Vim -h".
 */
#define VIM_VERSION_NODOT       "vim74"
#define VIM_VERSION_SHORT       "7.4"
#define VIM_VERSION_LONG        "VIM - Vi IMproved 7.4.692"
